package com.fra

class InputConfiguration(val codebookSize: Int, val localFeaturesCount: Int, inputClasses: Seq[InputClass]) {

	require(codebookSize > 10 && codebookSize < 1000)
	require(localFeaturesCount > 0 && localFeaturesCount < 100)

	val classes: List[InputClass] = inputClasses.toList

	require(classes.length > 1)

}

object InputConfiguration {

	import com.github.pathikrit.dijon.SomeJson
	import org.apache.spark.rdd.RDD
	import org.apache.spark.SparkContext

	val defaultCodebookSize: Int = 500
	val defaultLocalFeaturesCount: Int = 10
	private val defaultTestFraction: Double = 0.2

	private def resolveFiles(sparkContext : SparkContext, path: String, root: String): RDD[String] = {
		import java.nio.file.Paths
		import java.nio.file.Files
		import scala.collection.convert.ImplicitConversions.`iterable AsScalaIterable`
		val separatorIndex = path.lastIndexWhere(c => c == '/' || c == '\\')
		val directorySlice = path.substring(0, math.max(separatorIndex, 0))
		val nameSlice = path.substring(math.min(separatorIndex + 1, path.length - 1))
		val directory = Paths.get(root).resolve(Paths.get(directorySlice))
		val stream = Files.newDirectoryStream(directory, nameSlice)
		val files = for (path <- stream) yield path.toString
		stream.close()
		sparkContext.parallelize(files.toSeq)
	}

	private def resolveFiles(sparkContext: SparkContext, paths : Seq[String], root : String) : RDD[String] =
		sparkContext.union(paths.map(p => resolveFiles(sparkContext, p, root)))

	private def loadClass(sparkContext : SparkContext, root : String, config : SomeJson, testFraction : Double, testSeed : Option[Int]) : InputClass = {
		import scala.util.Random.nextInt
		val name = config.name.asString.get
		val trainFiles = resolveFiles(sparkContext, config.trainFiles.toSeq.map(_.asString.get), root)
		val testFiles = resolveFiles(sparkContext, config.testFiles.toSeq.map(_.asString.get), root)
		val mergedFiles = resolveFiles(sparkContext, config.files.toSeq.map(_.asString.get), root)
		val Array(mergedTrainFiles, mergedTestFiles) = mergedFiles.randomSplit(Array(testFraction, 1.0-testFraction), testSeed.getOrElse(nextInt).toLong)
		InputClass(name, (trainFiles union mergedTrainFiles).cache, (testFiles union mergedTestFiles).cache)
	}

	def load(sparkContext: SparkContext, configFile: String): InputConfiguration = {
		import scala.language.dynamics._
		import com.github.pathikrit.dijon._

		import java.nio.file.Paths
		import scala.io.Source
		val source = Source.fromFile(configFile)
		val json = try parse(source.mkString) finally source.close
		val codebookSize = json.codebookSize.asInt.getOrElse(defaultCodebookSize)
		val localFeaturesCount = json.localFeaturesCount.asInt.getOrElse(defaultLocalFeaturesCount)
		val testFraction = json.testFraction.asDouble.getOrElse(defaultTestFraction)
		val testSeed = json.testFraction.asInt
		val configFileRoot = Paths.get(configFile).getParent.toAbsolutePath.toString
		val classes = json.classes.toSeq.map(c => loadClass(sparkContext, configFileRoot, c, testFraction, testSeed))
		new InputConfiguration(codebookSize, localFeaturesCount, classes)
	}

}