package image_classifier.data

import org.apache.spark.sql.{DataFrame, SparkSession}
import image_classifier.data.DataLoader.{defaultClassCol, defaultImageCol}

final class DataLoader(workingDir: String, classCol: String, imageCol: String)(implicit spark: SparkSession) {

	import image_classifier.configuration.{DataConfig, DataSetConfig, JointDataConfig, Loader, SplitDataConfig}

	import java.nio.file.Paths
	def this(workingDir: String)(implicit spark: SparkSession) = this(workingDir, defaultClassCol, defaultImageCol)(spark)

	private def apply(config: DataConfig): Data = config match {
		case config: JointDataConfig => apply(config)
		case config: SplitDataConfig => apply(config)
	}

	private def apply(config: JointDataConfig): Data = {
		val data = apply(config.dataSet, config.tempDir)
		if (config.stratified) {
			val Array(training, test) = data.randomSplit(Array(1 - config.testFraction, config.testFraction), config.splitSeed)
			Data(training, test)
		} else
			???
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
			import image_classifier.utils.FileUtils
			val targetFile = config.mode match {
				case MakeAndSave | LoadOrMakeAndSave => file
				case _ => FileUtils.getTempHdfsFile(tempDir)
			}
			merge(config.config.get.classFiles, targetFile)
			data = Some(load(targetFile))
		}
		data.get
	}

	private def load(file: String): DataFrame = Merger.load(file, classCol, imageCol)

	private def tryLoad(file: String): Option[DataFrame] =
		try Some(load(file))
		catch {
			case _: Exception => None
		}

	private def merge(classFiles: Seq[Seq[String]], file: String): Unit = {
		import image_classifier.utils.FileUtils
		val explodedClassFiles = classFiles
			.map(_.flatMap(FileUtils.listFiles(workingDir, _)))
			.zipWithIndex
			.flatMap(zip => zip._1.map((zip._2, _)))
		Merger.mergeFiles(explodedClassFiles, file)
	}

}

object DataLoader {
	import image_classifier.configuration.{Config, DataConfig}
	import org.apache.spark.sql.DataFrame

	val defaultClassCol = "class"
	val defaultImageCol = "image"

	def apply(workingDir: String, config: DataConfig)(implicit spark: SparkSession): Data =
		apply(workingDir, config, defaultClassCol, defaultImageCol)(spark)

	def apply(workingDir: String, config: DataConfig, classCol: String, imageCol: String)(implicit spark: SparkSession): Data =
		new DataLoader(workingDir, classCol, imageCol)(spark)(config)

}
