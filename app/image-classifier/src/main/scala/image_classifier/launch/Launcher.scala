package image_classifier.launch

import image_classifier.configuration.Config
import image_classifier.pipeline.Pipeline
import image_classifier.utils.{FileUtils, SparkInstance}
import org.apache.log4j.Logger

object Launcher {

	private val logger: Logger = Logger.getLogger(getClass)

	def run(configFile: String): Unit = {
		logger.info(s"Launched with config file '$configFile'")
		val absConfigFile = FileUtils.resolve(System.getProperty("user.dir"), configFile)
		val workingDir = FileUtils.parent(absConfigFile)
		SparkInstance.execute(spark => {
			implicit val fileUtils = new FileUtils(workingDir)(spark)
			val config = Config.fromFile(absConfigFile)
			Pipeline.run(config)(spark, fileUtils)
		})
	}

	def run(config: Config, workingDir: String): Unit = {
		logger.info(s"Launched with config file inside '$workingDir'")
		SparkInstance.execute(spark => {
			val fileUtils = new FileUtils(workingDir)(spark)
			Pipeline.run(config)(spark, fileUtils)
		})
	}

}
