package image_classifier.pipeline

import image_classifier.utils.FileUtils

object Pipeline {
	import image_classifier.configuration.Config
	import Columns.colName
	import org.apache.spark.sql.SparkSession
	import org.apache.log4j.Logger

	private val logger = Logger.getLogger(getClass)

	private val isTestCol = colName("isTest")
	private val dataCol = colName("data")
	private val labelCol = colName("label")
	private val predictionCol = colName("prediction")

	def run(config: Config, workingDir: String)(implicit spark: SparkSession, fileUtils: FileUtils): Unit = {
		import image_classifier.pipeline.data.DataStage
		import org.apache.commons.lang.time.DurationFormatUtils
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
		val elapsed = DurationFormatUtils.formatDurationHMS((System.nanoTime - time) / 1000000L)
		logger.info(s"Pipeline ended after $elapsed")
	}

}
