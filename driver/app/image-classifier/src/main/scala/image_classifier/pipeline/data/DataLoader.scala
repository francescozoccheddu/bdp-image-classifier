package image_classifier.pipeline.data

import org.apache.spark.sql.{DataFrame, SparkSession}
import image_classifier.pipeline.data.DataLoader.{defaultLabelCol, defaultImageCol}
import image_classifier.pipeline.data.DataLoader.logger

private[pipeline] final class DataLoader(workingDir: String, labelCol: String, imageCol: String)(implicit spark: SparkSession) {

	import image_classifier.configuration.{DataConfig, DataSetConfig, JointDataConfig, Loader, SplitDataConfig}
	import image_classifier.pipeline.data.DataLoader.sparkListener

	import java.nio.file.Paths
	def this(workingDir: String)(implicit spark: SparkSession) = this(workingDir, defaultLabelCol, defaultImageCol)(spark)

	spark.sparkContext.removeSparkListener(sparkListener)
	spark.sparkContext.addSparkListener(sparkListener)

	private def apply(config: DataConfig): Data = config match {
		case config: JointDataConfig => apply(config)
		case config: SplitDataConfig => apply(config)
	}

	private def apply(config: JointDataConfig): Data = {
		val data = apply(config.dataSet, config.tempDir)
		if (config.stratified) {
			val Array(training, test) = data.randomSplit(Array(1 - config.testFraction, config.testFraction), config.splitSeed)
			Data(training, test)
		} else {
			logger.error("Stratified sampling is not implemented yet")
			??? // TODO Implement
		}
	}

	private def apply(config: SplitDataConfig): Data = Data(apply(config.trainingSet, config.tempDir), apply(config.testSet, config.tempDir))

	private def apply(config: Option[Loader[DataSetConfig]], tempDir: String): DataFrame = config.map(apply(_, tempDir)).orNull

	private def apply(config: Loader[DataSetConfig], tempDir: String): DataFrame = {
		import image_classifier.configuration.LoadMode._
		val file = config.file.orNull
		var data = config.mode match {
			case Load => Some(load(file))
			case LoadOrMake | LoadOrMakeAndSave => tryLoad(file)
			case _ => None
		}
		if (data.isEmpty) {
			val targetFile = config.mode match {
				case MakeAndSave | LoadOrMakeAndSave => file
				case _ => FileUtils.getTempHdfsFile(tempDir)
			}
			merge(config.config.get.classFiles, targetFile)
			data = Some(load(targetFile))
		}
		data.get
	}

	private def load(file: String): DataFrame = Merger.load(file, labelCol, imageCol)

	private def tryLoad(file: String): Option[DataFrame] =
		try Some(load(file))
		catch {
			case _: Exception => {
				logger.info(s"Failed to load '$file'")
				None
			}
		}

	private def merge(classFiles: Seq[Seq[String]], file: String): Unit = {
		val explodedClassFiles = classFiles
			.map(_.flatMap(FileUtils.listFiles(workingDir, _)))
			.zipWithIndex
			.flatMap(zip => zip._1.map((zip._2, _)))
		Merger.mergeFiles(explodedClassFiles, file)
	}

}

private[pipeline] object DataLoader {
	import image_classifier.configuration.{Config, DataConfig}
	import org.apache.log4j.Logger
	import org.apache.spark.scheduler.SparkListener
	import org.apache.spark.sql.DataFrame

	val defaultLabelCol = "label"
	val defaultImageCol = "image"

	private val logger = Logger.getLogger(DataLoader.getClass)

	def apply(workingDir: String, config: DataConfig)(implicit spark: SparkSession): Data =
		apply(workingDir, config, defaultLabelCol, defaultImageCol)(spark)

	def apply(workingDir: String, config: DataConfig, labelCol: String, imageCol: String)(implicit spark: SparkSession): Data =
		new DataLoader(workingDir, labelCol, imageCol)(spark)(config)

	private val sparkListener = new SparkListener {
		import org.apache.spark.scheduler.SparkListenerApplicationEnd

		override def onApplicationEnd(applicationEnd: SparkListenerApplicationEnd): Unit =
			FileUtils.clearTempFiles()

	}

}
