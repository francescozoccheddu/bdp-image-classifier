package image_classifier.pipeline.featurization

import image_classifier.configuration.{CodebookConfig, DescriptorConfig, FeaturizationConfig, Loader}
import image_classifier.pipeline.LoaderStage
import image_classifier.pipeline.data.DataStage
import image_classifier.pipeline.featurization.FeaturizationStage.{defaultOutputCol, logger}
import image_classifier.pipeline.utils.Columns.colName
import image_classifier.pipeline.utils.DataTypeImplicits.DataTypeExtension
import image_classifier.utils.FileUtils
import org.apache.log4j.Logger
import org.apache.spark.ml.linalg.SQLDataTypes.VectorType
import org.apache.spark.ml.linalg.Vector
import org.apache.spark.sql.functions.{col, countDistinct, size, udf}
import org.apache.spark.sql.types.{BooleanType, IntegerType}
import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}
import org.bytedeco.javacpp.{opencv_core, Loader => OpenCVLoader}

private[pipeline] final class FeaturizationStage(loader: Option[Loader[FeaturizationConfig]], val dataStage: DataStage, val outputCol: String = defaultOutputCol)(implicit spark: SparkSession, fileUtils: FileUtils)
  extends LoaderStage[DataFrame, FeaturizationConfig]("Featurization", loader)(fileUtils) {

	lazy val labelsCount: Int =
		result
		  .select(countDistinct(dataStage.labelCol))
		  .first
		  .getLong(0)
		  .toInt

	lazy val codebookSize: Int =
		result
		  .first
		  .getAs[Vector](outputCol)
		  .size

	override protected def make(): DataFrame = {
		val describedData = describe(config.descriptor, dataStage.result).cache
		logger.info(s"Succesfully described ${describedData.count} images")
		val codebook = createCodebook(config.codebook, describedData)
		logger.info("Computing BOVW")
		val bowv = BOWV.compute(describedData, codebook, outputCol).cache
		describedData.unpersist
		bowv
	}

	private def describe(config: DescriptorConfig, data: DataFrame): DataFrame = {
		logger.info("Extracting features")
		OpenCVLoader.load(classOf[opencv_core])
		val descriptor = Descriptor(config)
		val describe = udf(descriptor.apply(_, config.useImageIO))
		data
		  .withColumn(outputCol, describe(col(dataStage.imageCol)))
		  .filter(size(col(outputCol)) >= config.minFeatureCount)
		  .repartition(spark.sparkContext.defaultParallelism)
	}

	private def createCodebook(config: CodebookConfig, data: DataFrame): Seq[Vector] = {
		logger.info("Creating codebook")
		val training = data
		  .filter(!col(dataStage.isTestCol))
		  .repartition(spark.sparkContext.defaultParallelism)
		BOWV.createCodebook(training, outputCol, dataStage.labelCol, config)
	}

	protected override def validate(result: DataFrame): Unit = {
		val schema = result.schema
		schema.requireField(outputCol, VectorType)
		schema.requireField(dataStage.isTestCol, BooleanType)
		schema.requireField(dataStage.labelCol, IntegerType)
		require(schema.fields.length == 3)
	}

	override protected def load(): DataFrame = {
		spark.read.format("parquet").load(file).cache
	}

	override protected def save(result: DataFrame): Unit = result.write.format("parquet").mode(SaveMode.Overwrite).save(file)

}

private[pipeline] object FeaturizationStage {

	val defaultOutputCol: String = colName("features")

	private val logger: Logger = Logger.getLogger(getClass)

}