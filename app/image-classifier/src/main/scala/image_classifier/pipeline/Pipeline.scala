package image_classifier.pipeline

import image_classifier.configuration.Config
import image_classifier.pipeline.Columns.colName
import image_classifier.pipeline.data.DataStage
import image_classifier.pipeline.featurization.FeaturizationStage
import image_classifier.pipeline.testing.TestingStage
import image_classifier.pipeline.training.TrainingStage
import image_classifier.utils.FileUtils
import org.apache.commons.lang.time.DurationFormatUtils
import org.apache.log4j.Logger
import org.apache.spark.sql.SparkSession

object Pipeline {

	private val logger: Logger = Logger.getLogger(getClass)

	private val isTestCol: String = colName("isTest")
	private val dataCol: String = colName("data")
	private val labelCol: String = colName("label")
	private val predictionCol: String = colName("prediction")

	def run(config: Config, workingDir: String)(implicit spark: SparkSession, fileUtils: FileUtils): Unit = {
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
