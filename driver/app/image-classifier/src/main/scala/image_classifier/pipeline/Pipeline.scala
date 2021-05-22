package image_classifier.pipeline

object Pipeline {
	import image_classifier.configuration.Config
	import image_classifier.pipeline.utils.Columns.colName
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
		val data = new DataStage(config.data, workingDir, labelCol, isTestCol, dataCol)
		val featurization = new FeaturizationStage(config.featurization, data, dataCol)
		Seq(featurization, data).takeWhile(!_.hasResult)
		logger.info("Pipeline ended")
	}

}
