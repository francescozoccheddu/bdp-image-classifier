package image_classifier.pipeline

object Pipeline {
	import image_classifier.configuration.Config
	import image_classifier.pipeline.Columns.{colName, resColName}
	import org.apache.spark.sql.SparkSession
	import org.apache.log4j.Logger

	private val logger = Logger.getLogger(Pipeline.getClass)

	private val isTestCol = colName("isTest")
	private val dataCol = colName("data")
	private val labelCol = colName("label")

	def run(config: Config, workingDir: String)(implicit spark: SparkSession): Unit = {
		import image_classifier.pipeline.data.DataStage
		import image_classifier.pipeline.featurization.FeaturizationStage
		import image_classifier.utils.DataFrameImplicits._
		logger.info("Pipeline started")
		logger.info("Data stage")
		val data = config.data.map(DataStage(workingDir, _, labelCol, isTestCol, dataCol)).orNull
		logger.info("Featurization stage")
		val featurization = config.featurization.map(FeaturizationStage(_, data, dataCol, isTestCol, dataCol)).orNull
		logger.info("Training stage")
		logger.info("Testing stage")
		logger.info("Pipeline ended")
	}

}
