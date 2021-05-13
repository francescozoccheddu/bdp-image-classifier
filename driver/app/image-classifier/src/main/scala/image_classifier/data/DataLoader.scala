package image_classifier.data

import image_classifier.data.DataLoader.defaultTempFile
import org.apache.spark.sql.{DataFrame, SparkSession}
/*

final case class DataLoader(classCol: String, imageCol: String, tempFile: String = defaultTempFile)(implicit spark: SparkSession) {

	import image_classifier.configuration.LoadMode.LoadMode
	import image_classifier.data.DataLoader.{Data, defaultClassCol, defaultImageCol}
	import image_classifier.configuration.{DataSetConfig, JointDataConfig, Loader, SplitDataConfig}

	def this(tempFile: String = defaultTempFile) = this(defaultClassCol, defaultImageCol, tempFile)

	def apply(config: Loader[DataSetConfig]) = apply(config, defaultClassCol, defaultImageCol)

	def apply(config: Loader[DataSetConfig], classCol: String, imageCol: String): Data = {
		import image_classifier.configuration.{JointDataConfig, SplitDataConfig}
		val (training, test) = config match {
			case Loader(_, _, data: Option[JointDataConfig]) => null
			case Loader(_, _, data: Option[SplitDataConfig]) => null
		}
	}

	private def apply(config: JointDataConfig): Data = {
		val data = resolve(config.dataSet)

	}

	private def resolve(config: Loader[DataSetConfig]): DataFrame =
		resolve(config.mode, config.file.orNull, config.config.map(_.classFiles).orNull)

	private def resolve(mode: LoadMode, file: String, classFiles: Seq[Seq[String]]): DataFrame = {
		import image_classifier.configuration.LoadMode._
		var data = mode match {
			case Load => Some(load(file))
			case LoadOrMake | LoadOrMakeAndSave => tryLoad(file)
			case _ => None
		}
		if (data.isEmpty) {
			val target = mode match {
				case MakeAndSave | LoadOrMakeAndSave => Some(file)
				case _ => None
			}
			val targetFile = target.getOrElse(defaultTempFile)
			DataLoader.merge(classFiles, targetFile)
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
*/

object DataLoader {
	import org.apache.spark.sql.{DataFrame, SparkSession}

	val defaultClassCol = "class"
	val defaultImageCol = "image"
	val defaultTempFile = "hdfs://._image_classifier_temp"

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

}
