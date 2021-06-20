package com.github.francescozoccheddu.bdp_image_classifier.pipeline

import com.github.francescozoccheddu.bdp_image_classifier.configuration.Config
import com.github.francescozoccheddu.bdp_image_classifier.pipeline.data.DataStage
import com.github.francescozoccheddu.bdp_image_classifier.pipeline.featurization.FeaturizationStage
import com.github.francescozoccheddu.bdp_image_classifier.pipeline.testing.TestingStage
import com.github.francescozoccheddu.bdp_image_classifier.pipeline.training.TrainingStage
import com.github.francescozoccheddu.bdp_image_classifier.pipeline.utils.Columns.colName
import com.github.francescozoccheddu.bdp_image_classifier.utils.FileUtils
import org.apache.commons.lang.time.DurationFormatUtils
import org.apache.log4j.Logger
import org.apache.spark.sql.SparkSession

object Pipeline {

	private val logger: Logger = Logger.getLogger(getClass)

	private val isTestCol: String = colName("isTest")
	private val dataCol: String = colName("data")
	private val labelCol: String = colName("label")
	private val predictionCol: String = colName("prediction")

	def run(config: Config)(implicit spark: SparkSession, fileUtils: FileUtils): Unit = {
		logger.info("Pipeline started")
		val time = System.nanoTime
		val data = new DataStage(config.data, labelCol, isTestCol, dataCol)
		val featurization = new FeaturizationStage(config.featurization, data, dataCol)
		val training = new TrainingStage(config.training, featurization, predictionCol)
		val testing = new TestingStage(config.testing, training)
		Seq(testing, training, featurization, data).takeWhile(!_.hasResult)
		val elapsed = DurationFormatUtils.formatDurationHMS((System.nanoTime - time) / 1000000L)
		logger.info(s"Pipeline ended after $elapsed")
	}

}
