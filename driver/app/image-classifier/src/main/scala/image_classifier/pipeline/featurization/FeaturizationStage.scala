package image_classifier.pipeline.featurization

import image_classifier.configuration.{FeaturizationConfig, Loader}
import org.apache.spark.sql.{DataFrame, SparkSession}
import image_classifier.pipeline.featurization.FeaturizationStage.defaultOutputCol
import image_classifier.pipeline.LoaderStage
import image_classifier.pipeline.data.DataStage

private[pipeline] final class FeaturizationStage(loader: Option[Loader[FeaturizationConfig]], dataStage: DataStage, outputCol: String = defaultOutputCol)(implicit spark: SparkSession) extends LoaderStage[DataFrame, FeaturizationConfig]("Featurization", loader) {
	import org.apache.spark.sql.types.DataType

	private def validate(schema: DataType) = {
		import image_classifier.utils.DataTypeImplicits.DataTypeExtension
		import org.apache.spark.sql.types.{BinaryType, BooleanType}
		schema.requireField(dataStage.imageCol, BinaryType)
		schema.requireField(dataStage.isTestCol, BooleanType)
	}

	private def describe(config: FeaturizationConfig, data: DataFrame) = {
		import org.apache.spark.sql.functions.{col, udf}
		val descriptor = Descriptor(config.featureCount, config.algorithm)
		val describe = udf((d: Array[Byte]) => descriptor(Image.limitSize(Image.decode(d), config.maxSize)))
		data.withColumn(outputCol, describe(col(dataStage.imageCol)))
	}

	private def createCodebook(config: FeaturizationConfig, data: DataFrame) = {
		import org.apache.spark.sql.functions.{col, explode}
		val training = data.filter(!col(dataStage.isTestCol)).withColumn(outputCol, explode(col(outputCol)))
		BOWV.createCodebook(training, config.codebookSize, outputCol, config.assignNearest)
	}

	override protected def make(config: FeaturizationConfig) = {
		validate(dataStage.result.schema)
		val describedData = describe(config, dataStage.result).cache
		val codebook = createCodebook(config, describedData)
		val bowv = BOWV.compute(describedData, codebook, config.codebookSize, outputCol)
		describedData.unpersist
		bowv
	}

	override protected def load(file: String) =
		spark.read.format("parquet").load(file)

	override protected def save(result: DataFrame, file: String): Unit = result.write.format("parquet").save(file)

}

private[pipeline] object FeaturizationStage {
	import image_classifier.pipeline.Columns.colName

	val defaultOutputCol = colName("features")

}