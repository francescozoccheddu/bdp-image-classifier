package image_classifier.pipeline

object Pipeline {
	import image_classifier.configuration.Config
	import org.apache.spark.sql.SparkSession
	import org.apache.log4j.Logger

	private val logger = Logger.getLogger(Pipeline.getClass)

	def run(config: Config, workingDir: String)(implicit spark: SparkSession): Unit = {
		import image_classifier.pipeline.data.DataLoader
		logger.info("Pipeline started")
		logger.info("Data stage")
		val data = config.data.map(DataLoader(workingDir, _))
		logger.info("Featurization stage")
		logger.info("Training stage")
		logger.info("Testing stage")
		logger.info("Pipeline ended")
	}

}
