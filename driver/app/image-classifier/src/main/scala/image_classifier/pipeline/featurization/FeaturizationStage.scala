package image_classifier.pipeline.featurization

import org.apache.spark.sql.{SparkSession, DataFrame}
import image_classifier.pipeline.featurization.FeaturizationStage.defaultOutputCol

private[pipeline] final class FeaturizationStage(inputCol: String, isTestCol: String, outputCol: String)(implicit spark: SparkSession) {
	import image_classifier.configuration.{FeaturizationConfig, Loader}
	import image_classifier.pipeline.featurization.FeaturizationStage.logger
	import image_classifier.utils.Files

	def this(inputCol: String, isTestCol: String)(implicit spark: SparkSession) = this(inputCol, isTestCol, defaultOutputCol)(spark)

	def apply(loader: Loader[FeaturizationConfig], data: DataFrame): DataFrame = {
		import image_classifier.utils.OptionImplicits._
		import image_classifier.configuration.LoadMode
		validate(data)
		logger.info(s"Running $loader")
		loader.mode match {
			case LoadMode.Load => load(loader.file.get)
			case LoadMode.LoadOrMake => loadIfExists(loader.file.get).getOr(() => make(loader.config.get, data))
			case LoadMode.Make => make(loader.config.get, data)
			case LoadMode.MakeAndSave => makeAndSave(loader.config.get, data, loader.file.get)
			case LoadMode.LoadOrMakeAndSave => loadIfExists(loader.file.get).getOr(() => makeAndSave(loader.config.get, data, loader.file.get))
		}
	}

	private def validate(data: DataFrame) = {
		if (data != null) {
			import image_classifier.utils.DataTypeImplicits._
			import org.apache.spark.sql.types.{BinaryType, BooleanType}
			val schema = data.schema
			schema.requireField(inputCol, BinaryType)
			schema.requireField(isTestCol, BooleanType)
		}
	}

	private def describe(config: FeaturizationConfig, data: DataFrame) = {
		import org.apache.spark.sql.functions.{col, udf}
		val descriptor = Descriptor(config.featureCount, config.algorithm)
		val describe = udf((d: Array[Byte]) => descriptor(Image.limitSize(Image.decode(d), config.maxSize)))
		data.withColumn(outputCol, describe(col(inputCol)))
	}

	private def createCodebook(config: FeaturizationConfig, data: DataFrame) = {
		import org.apache.spark.sql.functions.{col, explode}
		val training = data.filter(!col(isTestCol)).withColumn(outputCol, explode(col(outputCol)))
		BOWV.createCodebook(training, config.codebookSize, outputCol, config.assignNearest)
	}

	private def makeAndSave(config: FeaturizationConfig, data: DataFrame, file: String) = {
		val made = make(config, data)
		FeaturizationStage.save(data, file)
		made
	}

	private def make(config: FeaturizationConfig, data: DataFrame) = {
		logger.info(s"Making config")
		val describedData = describe(config, data).cache
		val codebook = createCodebook(config, describedData)
		val bowv = BOWV.compute(describedData, codebook, config.codebookSize, outputCol)
		import image_classifier.utils.DataFrameImplicits._
		bowv.print()
		describedData.unpersist
		bowv
	}

	private def load(file: String) = {
		logger.info(s"Loading '$file'")
		spark.read.format("parquet").load(file)
	}

	private def loadIfExists(file: String) =
		if (Files.exists(file))
			Some(load(file))
		else {
			logger.info("File '$file' does not exist")
			None
		}

}

private[pipeline] object FeaturizationStage {
	import image_classifier.configuration.{FeaturizationConfig, Loader}
	import image_classifier.pipeline.Columns.colName
	import org.apache.log4j.Logger

	val defaultOutputCol = colName("features")

	private val logger = Logger.getLogger(FeaturizationStage.getClass)

	private def save(data: DataFrame, file: String) = data.write.format("parquet").save(file)

	def apply(loader: Loader[FeaturizationConfig], data: DataFrame, inputCol: String, isTestCol: String)(implicit spark: SparkSession): DataFrame =
		apply(loader, data, inputCol, isTestCol, defaultOutputCol)(spark)

	def apply(loader: Loader[FeaturizationConfig], data: DataFrame, inputCol: String, isTestCol: String, outputCol: String)(implicit spark: SparkSession): DataFrame =
		new FeaturizationStage(inputCol, isTestCol, outputCol)(spark)(loader, data)

}