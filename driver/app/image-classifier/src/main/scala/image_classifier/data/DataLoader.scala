package image_classifier.data

import org.apache.spark.sql.{DataFrame, SparkSession}
import image_classifier.data.DataLoader.{Data, defaultClassCol, defaultImageCol}

final case class DataLoader(classCol: String, imageCol: String)(implicit spark: SparkSession) {

	import image_classifier.configuration.{DataConfig, DataSetConfig, JointDataConfig, Loader, SplitDataConfig}
	def this()(implicit spark: SparkSession) = this(defaultClassCol, defaultImageCol)(spark)

	private def apply(config: DataConfig): Data = config match {
		case config: JointDataConfig => apply(config)
		case config: SplitDataConfig => apply(config)
	}

	private def apply(config: JointDataConfig): Data = {
		val data = apply(config.dataSet, config.tempFile)
		if (config.stratified) {
			val Array(training, test) = data.randomSplit(Array(1 - config.testFraction, config.testFraction), config.splitSeed)
			Data(training, test)
		} else
			???
	}

	private def apply(config: SplitDataConfig): Data = Data(apply(config.trainingSet, config.tempFile), apply(config.testSet, config.tempFile))

	private def apply(config: Option[Loader[DataSetConfig]], tempFile: String): DataFrame = config match {
		case Some(config) => apply(config, tempFile)
		case None => spark.emptyDataFrame
	}

	private def apply(config: Loader[DataSetConfig], tempFile: String): DataFrame = {
		import image_classifier.configuration.LoadMode._
		val file = config.file.orNull
		var data = config.mode match {
			case Load => Some(load(file))
			case LoadOrMake | LoadOrMakeAndSave => tryLoad(file)
			case _ => None
		}
		if (data.isEmpty) {
			val target = config.mode match {
				case MakeAndSave | LoadOrMakeAndSave => Some(file)
				case _ => None
			}
			val targetFile = target.getOrElse(tempFile)
			DataLoader.merge(config.config.get.classFiles, targetFile)
			data = Some(load(targetFile))
			if (target.isEmpty)
				DataLoader.delete(targetFile)
		}
		data.get
	}

	private def load(file: String): DataFrame = Merger.load(file, classCol, imageCol)

	private def tryLoad(file: String): Option[DataFrame] =
		try Some(load(file))
		catch {
			case _: Exception => None
		}

}

object DataLoader {
	import image_classifier.configuration.{Config, DataConfig}
	import org.apache.spark.sql.DataFrame

	val defaultClassCol = "class"
	val defaultImageCol = "image"

	case class Data(training: DataFrame, test: DataFrame)

	private def merge(classFiles: Seq[Seq[String]], file: String): Unit = {
		val tuples = classFiles.zipWithIndex.flatMap { case (f, c) => f.map((c, _)) }
		Merger.mergeFiles(tuples, file)
	}

	private def delete(file: String) = {
		import org.apache.hadoop.fs.{FileSystem, Path}
		import org.apache.hadoop.conf.Configuration
		FileSystem.get(new Configuration).delete(new Path(file), false)
	}

	def apply(config: DataConfig)(implicit spark: SparkSession): Data =
		apply(config, defaultClassCol, defaultImageCol)(spark)

	def apply(config: DataConfig, classCol: String, imageCol: String)(implicit spark: SparkSession): Data =
		new DataLoader(classCol, imageCol)(spark)(config)

}
