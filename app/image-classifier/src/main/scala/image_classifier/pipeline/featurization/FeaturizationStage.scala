package image_classifier.pipeline.featurization

import com.github.dwickern.macros.NameOf.nameOf
import image_classifier.configuration.{CodebookConfig, DescriptorConfig, FeaturizationConfig, Loader}
import image_classifier.pipeline.Columns.colName
import image_classifier.pipeline.LoaderStage
import image_classifier.pipeline.data.DataStage
import image_classifier.pipeline.featurization.FeaturizationStage.{defaultOutputCol, logger}
import image_classifier.utils.DataTypeImplicits.DataTypeExtension
import image_classifier.utils.FileUtils
import image_classifier.utils.OptionImplicits.OptionExtension
import org.apache.log4j.Logger
import org.apache.spark.ml.linalg.SQLDataTypes.VectorType
import org.apache.spark.ml.linalg.Vector
import org.apache.spark.sql.functions.{col, countDistinct, explode, udf}
import org.apache.spark.sql.types.{BooleanType, IntegerType}
import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}

private[pipeline] final class FeaturizationStage(loader: Option[Loader[FeaturizationConfig]], val dataStage: DataStage, val outputCol: String = defaultOutputCol)(implicit spark: SparkSession, fileUtils: FileUtils)
  extends LoaderStage[DataFrame, FeaturizationConfig]("Featurization", loader)(fileUtils) {

	private lazy val computedLabelsCount: Int = if (canTrustConfig && config.codebook.labelsCount.isDefined) {
		logger.info(s"Trusting '${nameOf(config.codebook.labelsCount)}'")
		config.codebook.labelsCount.get
	} else {
		logger.warn(s"Computing labels count. This may take a while. Please consider specifying it manually where needed.")
		val count = result.select(countDistinct(dataStage.labelCol)).first.getLong(0).toInt
		logger.info(s"Labels count is $count")
		count
	}

	private var codebookSizeOpt: Option[Int] = None

	def codebookSize: Int = codebookSizeOpt.getOr(() => {
		result
		codebookSizeOpt.get
	})

	override protected def make(): DataFrame = {
		val describedData = describe(config.descriptor, dataStage.result).cache
		val codebook = createCodebook(config.codebook, describedData)
		logger.info("Computing BOVW")
		val bowv = BOWV.compute(describedData, codebook, outputCol).cache
		describedData.unpersist
		bowv
	}

	private def describe(config: DescriptorConfig, data: DataFrame): DataFrame = {
		logger.info("Extracting features")
		val descriptor = Descriptor(config)
		val describe = udf(descriptor.apply: Array[Byte] => Seq[Vector])
		data.withColumn(outputCol, describe(col(dataStage.imageCol)))
	}

	private def createCodebook(config: CodebookConfig, data: DataFrame): Seq[Vector] = {
		logger.info(s"Creating a codebook of size ${config.size} out of ${"%.3f".format(config.sampleFraction * 100)}% of data")
		val training = data
		  .filter(!col(dataStage.isTestCol))
		  .withColumn(outputCol, explode(col(outputCol)))
		  .repartition(spark.sparkContext.defaultParallelism)
		val sample =
			if (config.stratifiedSampling) {
				val fractions = (0 until config.labelsCount.getOr(() => labelsCount)).map((_, config.sampleFraction)).toMap
				training.stat.sampleBy(dataStage.labelCol, fractions, config.sampleSeed.toLong)
			} else
				training.sample(config.sampleFraction, config.sampleSeed)
		val codebook = BOWV.createCodebook(sample, outputCol, config)
		codebookSizeOpt = Some(codebook.length)
		codebook
	}

	def labelsCount: Int =
		if (canTrustConfig && dataStage.wasMade)
			dataStage.config.labelsCount
		else
			computedLabelsCount

	private def canTrustConfig: Boolean = wasMade || loadMode.alwaysDoesMake

	protected override def validate(result: DataFrame): Unit = {
		val schema = result.schema
		schema.requireField(outputCol, VectorType)
		schema.requireField(dataStage.isTestCol, BooleanType)
		schema.requireField(dataStage.labelCol, IntegerType)
		require(schema.fields.length == 3)
	}

	override protected def load(): DataFrame = {
		if (!FileUtils.isValidHDFSPath(file))
			logger.warn("Loading from a local path hampers parallelization")
		spark.read.format("parquet").load(file).cache
	}

	override protected def save(result: DataFrame): Unit = result.write.format("parquet").mode(SaveMode.Overwrite).save(file)

}

private[pipeline] object FeaturizationStage {

	val defaultOutputCol: String = colName("features")

	private val logger: Logger = Logger.getLogger(getClass)

}