package image_classifier.input

import image_classifier.Pipeline.{dataColName, isTestColName, classColName}
import org.apache.spark.sql.{SparkSession, DataFrame}
import org.apache.spark.ml.image.ImageSchema

case class Input private (options : InputOptions, data : DataFrame, classes : Seq[String]) {
	
	require(classes.nonEmpty)
	
}

object Input {
	
	private val imageColName = ImageSchema.imageSchema.fieldNames(0)
	
	def loadFromConfigFile(spark : SparkSession, configFile : String) : Input = {
		val workingDir = java.nio.file.Paths.get(configFile).getParent.toString
		load(spark, InputConfiguration.fromFile(configFile), workingDir)
	}
	
	private[input] def load(spark : SparkSession, config : InputConfiguration, workingDir : String) : Input = {
		import org.apache.spark.sql.functions.lit
		val classesData = for ((c, i) <- config.classes.zipWithIndex) yield loadClass(spark, config, c, workingDir).withColumn(classColName, lit(i))
		val data = classesData.reduce(_ union _).withColumnRenamed(imageColName, dataColName)
		val classNames = config.classes.map(_.name).toSeq
		Input(config.options, data, classNames)
	}
	
	private def loadClass(spark : SparkSession, config : InputConfiguration, classConfig : InputClass, workingDir : String) : DataFrame = {
		val trainImages = loadImages(spark, classConfig.trainFiles, workingDir)
		val testImages = loadImages(spark, classConfig.testFiles, workingDir)
		val mergedImages = loadImages(spark, classConfig.files, workingDir)
		val testSeed = config.testSeed.getOrElse(util.Random.nextInt)
		val Array(mergedTrainImages, mergedTestImages) = mergedImages.randomSplit(Array(1.0 - config.testFraction, config.testFraction), testSeed)
		mergeImages(trainImages union mergedTrainImages, testImages union mergedTestImages)
	}
	
	private def mergeImages(trainImages : DataFrame, testImages : DataFrame) : DataFrame = {
		import org.apache.spark.sql.functions.lit
		trainImages.withColumn(isTestColName, lit(false)) union testImages.withColumn(isTestColName, lit(true))
	}

	private def loadImages(spark : SparkSession, paths : Seq[String], workingDir : String) : DataFrame = {
		if (paths.nonEmpty) {
			val resolvedPaths = paths.map(p => if (new java.io.File(p).isAbsolute) p else s"${workingDir}${java.io.File.separatorChar}${p}")
			spark.read.format("image").option("dropInvalid", true).load(resolvedPaths : _*)
		}
		else {
			import org.apache.spark.sql.Row
			import java.util.ArrayList
			spark.createDataFrame(new ArrayList[Row], ImageSchema.imageSchema)
		}
	}

}