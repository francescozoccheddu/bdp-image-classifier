package image_classifier.input

import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.lit

case class Input private (options : InputOptions, data : DataFrame, classes : Seq[String]) {
	
	require(classes.nonEmpty)
	
}

object Input {
	
	val isTestCol : String = "isTest"
	val classCol : String = "class"
	val imageCol : String = "image"
	
	def loadFromConfigFile(configFile : String, spark : SparkSession) : Input = {
		val workingDir = java.nio.file.Paths.get(configFile).getParent.toString
		load(InputConfiguration.fromFile(configFile), workingDir, spark)
	}

	private[input] def load(config : InputConfiguration, workingDir : String, spark : SparkSession) : Input = {
		val classesData = for ((c, i) <- config.classes.zipWithIndex) yield loadClass(config, c, workingDir, spark).withColumn(classCol, lit(i))
		val data = classesData.reduce(_ union _)
		val classNames = config.classes.map(_.name).toSeq
		Input(config.options, data, classNames)
	}
	
	private def loadClass(config : InputConfiguration, classConfig : InputClass, workingDir : String, spark : SparkSession) : DataFrame = {
		val trainImages = loadImages(classConfig.trainFiles, workingDir, spark)
		val testImages = loadImages(classConfig.testFiles, workingDir, spark)
		val mergedImages = loadImages(classConfig.files, workingDir, spark)
		val testSeed = config.testSeed.getOrElse(util.Random.nextInt)
		val Array(mergedTrainImages, mergedTestImages) = mergedImages.randomSplit(Array(1.0 - config.testFraction, config.testFraction), testSeed)
		mergeImages(trainImages union mergedTrainImages, testImages union mergedTestImages)
	}
	
	private def mergeImages(trainImages : DataFrame, testImages : DataFrame) : DataFrame = {
		trainImages.withColumn(isTestCol, lit(false)) union testImages.withColumn(isTestCol, lit(true))
	}

	private def loadImages(paths : Seq[String], workingDir : String, spark : SparkSession) : DataFrame = {
		if (paths.nonEmpty) {
			val resolvedPaths = paths.map(p => if (new java.io.File(p).isAbsolute) p else s"${workingDir}${java.io.File.separatorChar}${p}")
			spark.read.format("image").option("dropInvalid", true).load(resolvedPaths : _*)
		}
		else {
			import org.apache.spark.ml.image.ImageSchema
			import org.apache.spark.sql.Row
			import java.util.ArrayList
			spark.createDataFrame(new ArrayList[Row], ImageSchema.imageSchema)
		}
	}

}