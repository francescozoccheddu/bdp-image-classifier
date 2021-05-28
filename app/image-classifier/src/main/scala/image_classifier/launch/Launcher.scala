package image_classifier.launch

import image_classifier.pipeline.Pipeline
import image_classifier.utils.{FileUtils, SparkInstance}
import org.apache.log4j.Logger

object Launcher {

	import image_classifier.configuration.Config

	private val logger = Logger.getLogger(getClass)

	def run(configFile: String): Unit = {
		logger.info(s"Launched with config file '$configFile'")
		val workingDir = FileUtils.parent(configFile)
		SparkInstance.execute(spark => {
			implicit val fileUtils = new FileUtils()(spark)
			val config = Config.fromFile(configFile)
			Pipeline.run(config, workingDir)(spark, fileUtils)
		})
	}

	def run(config: Config, workingDir: String): Unit = {
		logger.info(s"Launched with config file inside '$workingDir'")
		SparkInstance.execute(spark => {
			val fileUtils = new FileUtils()(spark)
			Pipeline.run(config, workingDir)(spark, fileUtils)
		})
	}

}
