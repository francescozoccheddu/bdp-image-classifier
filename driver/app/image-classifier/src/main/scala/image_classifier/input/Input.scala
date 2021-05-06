package image_classifier.input

import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.lit

case class Input private (options : InputOptions, data : DataFrame, classes : Seq[String]) {
    
    require(classes.nonEmpty)
    
}

object Input {
    
    val isTestColumn : String = "isTest"
    val classColumn : String = "class"
    
    def loadFromConfigFile(configFile : String, spark : SparkSession) : Input = {
        val workingDir = java.nio.file.Paths.get(configFile).getParent.toString
        load(InputConfiguration.fromFile(configFile), workingDir, spark)
    }

    private[input] def load(config : InputConfiguration, workingDir : String, spark : SparkSession) : Input = {
        val classesData = for ((c, i) <- config.classes.zipWithIndex) yield loadClass(config, c, workingDir, spark).withColumn(classColumn, lit(i))
        val data = classesData.reduce(_ union _)
        val classNames = config.classes.map(_.name).toSeq
        Input(config.toOptions, data, classNames)
    }
    
    private def loadClass(config : InputConfiguration, classConfig : InputClass, workingDir : String, spark : SparkSession) : DataFrame = {
        val trainImages = loadImages(classConfig.trainFiles, workingDir, spark)
        val testImages = loadImages(classConfig.testFiles, workingDir, spark)
        val mergedImages = loadImages(classConfig.mergedFiles, workingDir, spark)
        val testSeed = config.testSeed.getOrElse(util.Random.nextInt)
        val Array(mergedTrainImages, mergedTestImages) = mergedImages.randomSplit(Array(1.0 - config.testFraction, config.testFraction), testSeed)
        mergeImages(trainImages union mergedTrainImages, testImages union mergedTestImages)
    }
    
    private def mergeImages(trainImages : DataFrame, testImages : DataFrame) : DataFrame = {
        trainImages.withColumn(isTestColumn, lit(false))
        testImages.withColumn(isTestColumn, lit(true))
        trainImages union testImages
    }

    private def loadImages(paths : Seq[String], workingDir : String, spark : SparkSession) : DataFrame = {
        val resolvedPaths = paths.map(p => if (new java.io.File(p).isAbsolute) p else s"${workingDir}${java.io.File.separatorChar}${p}")
        spark.read.format("image").option("dropInvalid", true).load(resolvedPaths : _*)
    }

}