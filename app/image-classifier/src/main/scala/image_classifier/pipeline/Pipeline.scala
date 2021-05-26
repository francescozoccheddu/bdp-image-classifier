package image_classifier.pipeline

object Pipeline {
	import image_classifier.configuration.Config
	import image_classifier.pipeline.utils.Columns.colName
	import org.apache.spark.sql.SparkSession
	import org.apache.log4j.Logger

	private val logger = Logger.getLogger(getClass)

	private val isTestCol = colName("isTest")
	private val dataCol = colName("data")
	private val labelCol = colName("label")
	private val predictionCol = colName("prediction")

	def run(config: Config, workingDir: String)(implicit spark: SparkSession): Unit = {
		import image_classifier.pipeline.data.DataStage
		import image_classifier.pipeline.featurization.FeaturizationStage
		import image_classifier.pipeline.testing.TestingStage
		import image_classifier.pipeline.training.TrainingStage

		logger.info("Pipeline started")
		val time = System.nanoTime
		val data = new DataStage(config.data, workingDir, labelCol, isTestCol, dataCol)
		val featurization = new FeaturizationStage(config.featurization, data, dataCol)
		val training = new TrainingStage(config.training, featurization, predictionCol)
		val testing = new TestingStage(config.testing, training)
		Seq(testing, training, featurization, data).takeWhile(!_.hasResult)
		val elapsed = (System.nanoTime - time) / 1000000000.0
		logger.info(s"Pipeline ended after ${"%.3f".format(elapsed)}s")
	}

}
