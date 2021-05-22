package image_classifier.pipeline.data

import org.apache.spark.sql.{DataFrame, SparkSession}
import image_classifier.configuration.{DataConfig, Loader}
import image_classifier.pipeline.data.DataLoader.{defaultLabelCol, defaultIsTestCol, defaultImageCol}

private[pipeline] final class DataLoader(workingDir: String, labelCol: String, isTestCol: String, imageCol: String)(implicit spark: SparkSession) {
	import image_classifier.pipeline.data.DataLoader.{logger, encode}

	def this(workingDir: String)(implicit spark: SparkSession) = this(workingDir, defaultLabelCol, defaultIsTestCol, defaultImageCol)(spark)

	def apply(loader: Loader[DataConfig]): DataFrame = {
		import image_classifier.utils.OptionImplicits._
		import image_classifier.configuration.LoadMode
		logger.info(s"Running $loader")
		loader.mode match {
			case LoadMode.Load => load(loader.file.get)
			case LoadMode.LoadOrMake => tryLoad(loader.file.get).getOr(() => make(loader.config.get, None))
			case LoadMode.Make => make(loader.config.get, None)
			case LoadMode.MakeAndSave => make(loader.config.get, loader.file)
			case LoadMode.LoadOrMakeAndSave => tryLoad(loader.file.get).getOr(() => make(loader.config.get, loader.file))
		}
	}

	private def make(config: DataConfig, save: Option[String]) = {
		val file = save.getOrElse(config.tempFile)
		logger.info(s"Making config into '$file'")
		val sources = Array.ofDim[Option[Seq[(Int, String)]]](3)
		sources(0) = config.dataSet.map(encodeFiles(_, config.testFraction, config.splitSeed, config.stratified))
		sources(1) = config.trainingSet.map(encodeFiles(_, false))
		sources(2) = config.testSet.map(encodeFiles(_, true))
		if (save.isEmpty)
			addTempFile(file)
		Merger.mergeFiles(sources.flatten.flatten.toSeq, file)
		load(file)
	}

	private def load(file: String): DataFrame = {
		import image_classifier.pipeline.data.DataLoader.{keyCol, dataCol}
		import org.apache.spark.sql.functions.{col, abs}
		logger.info(s"Loading '$file'")
		Merger.load(file, keyCol, dataCol)
			.select(
				col(dataCol).alias(imageCol),
				(col(keyCol) < 0).alias(isTestCol),
				(abs(col(keyCol)) - 1).alias(labelCol))
	}

	private def tryLoad(file: String): Option[DataFrame] =
		try Some(load(file))
		catch {
			case _: Exception =>
				logger.info(s"Failed to load '$file'")
				None
		}

	private def resolveFiles(classFiles: Seq[Seq[String]]) =
		classFiles
			.map(_.flatMap(FileUtils.listFiles(workingDir, _)))
			.zipWithIndex

	private def explodeFiles(classFiles: Seq[Seq[String]]): Seq[(Int, String)] =
		resolveFiles(classFiles)
			.flatMap(zip => zip._1.map((zip._2, _)))

	private def encodeFiles(classFiles: Seq[Seq[String]], isTest: Boolean): Seq[(Int, String)] =
		explodeFiles(classFiles)
			.map(p => encode(p, isTest))

	private def encodeFiles(classFiles: Seq[Seq[String]], testFraction: Double, testSeed: Int): Seq[(Int, String)] =
		encode(explodeFiles(classFiles), testFraction, testSeed)

	private def encodeFiles(classFiles: Seq[Seq[String]], testFraction: Double, testSeed: Int, stratified: Boolean): Seq[(Int, String)] =
		if (stratified)
			encodeFilesStratified(classFiles, testFraction, testSeed)
		else
			encodeFiles(classFiles, testFraction, testSeed)

	private def encodeFilesStratified(classFiles: Seq[Seq[String]], testFraction: Double, testSeed: Int): Seq[(Int, String)] =
		resolveFiles(classFiles)
			.flatMap(zip => encode(zip._1.map((zip._2, _)), testFraction, testSeed))

	private def addTempFile(file: String) = {
		import image_classifier.pipeline.data.DataLoader.sparkListener
		spark.sparkContext.removeSparkListener(sparkListener)
		spark.sparkContext.addSparkListener(sparkListener)
		FileUtils.addTempFile(file)
	}

}

private[pipeline] object DataLoader {
	import org.apache.log4j.Logger
	import org.apache.spark.scheduler.SparkListener

	val defaultLabelCol = "label"
	val defaultIsTestCol = "isTest"
	val defaultImageCol = "image"

	private val keyCol = "key"
	private val dataCol = "data"

	private val logger = Logger.getLogger(DataLoader.getClass)

	private def encode(file: (Int, String), isTest: Boolean): (Int, String) = {
		val (label, path) = file
		if (isTest)
			(-label - 1, path)
		else
			(label + 1, path)
	}

	private def encode(files: Seq[(Int, String)], testFraction: Double, testSeed: Int): Seq[(Int, String)] = {
		import scala.util.Random
		val count = files.length * testFraction
		new Random(testSeed)
			.shuffle(files)
			.zipWithIndex
			.map(p => encode(p._1, p._2 < count))
	}

	def apply(workingDir: String, loader: Loader[DataConfig])(implicit spark: SparkSession): DataFrame =
		apply(workingDir, loader, defaultLabelCol, defaultIsTestCol, defaultImageCol)(spark)

	def apply(workingDir: String, loader: Loader[DataConfig], labelCol: String, isTestCol: String, imageCol: String)(implicit spark: SparkSession): DataFrame =
		new DataLoader(workingDir, labelCol, isTestCol, imageCol)(spark)(loader)

	private val sparkListener = new SparkListener {
		import org.apache.spark.scheduler.SparkListenerApplicationEnd

		override def onApplicationEnd(applicationEnd: SparkListenerApplicationEnd): Unit =
			FileUtils.clearTempFiles()

	}

}
