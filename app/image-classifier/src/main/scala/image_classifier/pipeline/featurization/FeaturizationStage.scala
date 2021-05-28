package image_classifier.pipeline.featurization

import image_classifier.configuration.{FeaturizationConfig, Loader}
import org.apache.spark.sql.{DataFrame, SparkSession}
import image_classifier.pipeline.featurization.FeaturizationStage.defaultOutputCol
import image_classifier.pipeline.LoaderStage
import image_classifier.pipeline.data.DataStage
import image_classifier.pipeline.featurization.FeaturizationStage.logger
import image_classifier.utils.FileUtils

private[pipeline] final class FeaturizationStage(loader: Option[Loader[FeaturizationConfig]], val dataStage: DataStage, val outputCol: String = defaultOutputCol)(implicit spark: SparkSession, fileUtils: FileUtils)
  extends LoaderStage[DataFrame, FeaturizationConfig]("Featurization", loader)(fileUtils) {
	import org.apache.spark.sql.types.DataType

	private def validate(schema: DataType) = {
		import image_classifier.utils.DataTypeImplicits.DataTypeExtension
		import org.apache.spark.sql.types.{BinaryType, BooleanType}
		schema.requireField(dataStage.imageCol, BinaryType)
		schema.requireField(dataStage.isTestCol, BooleanType)
	}

	private def describe(config: FeaturizationConfig, data: DataFrame) = {
		import org.apache.spark.sql.functions.{col, udf}
		val descriptor = Descriptor(config)
		val describe = udf((d: Array[Byte]) => descriptor(Image.limitSize(Image.decode(d), config.maxSize)))
		data.withColumn(outputCol, describe(col(dataStage.imageCol)))
	}

	private def createCodebook(config: FeaturizationConfig, data: DataFrame) = {
		import org.apache.spark.sql.functions.{col, explode}
		val training = data.filter(!col(dataStage.isTestCol)).withColumn(outputCol, explode(col(outputCol)))
		BOWV.createCodebook(training, config.codebookSize, outputCol, config.assignNearest)
	}

	override protected def make(config: FeaturizationConfig) = {
		import image_classifier.pipeline.featurization.FeaturizationStage.logger
		validate(dataStage.result.schema)
		logger.info("Extracting features")
		val describedData = describe(config, dataStage.result).cache
		logger.info("Creating codebook")
		val codebook = createCodebook(config, describedData)
		logger.info("Computing BOVW")
		val bowv = BOWV.compute(describedData, codebook, config.codebookSize, outputCol)
		bowv
	}

	override protected def load(file: String) = {
		if (!FileUtils.isValidHDFSPath(file))
			logger.warn("Loading from a local path hampers parallelization")
		spark.read.format("parquet").load(file)
	}

	override protected def save(result: DataFrame, file: String): Unit = result.write.format("parquet").save(file)

}

private[pipeline] object FeaturizationStage {
	import image_classifier.pipeline.Columns.colName
	import org.apache.log4j.Logger

	val defaultOutputCol = colName("features")

	private val logger = Logger.getLogger(getClass)

}