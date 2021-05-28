package image_classifier.launch

import image_classifier.pipeline.Pipeline
import image_classifier.pipeline.utils.FileUtils
import image_classifier.utils.SparkInstance

object Launcher {

	import image_classifier.configuration.Config

	def run(configFile: String): Unit = {
		val workingDir = FileUtils.parent(configFile)
		SparkInstance.execute(spark => {
			implicit val fileUtils = new FileUtils()(spark)
			val config = Config.fromFile(configFile)
			Pipeline.run(config, workingDir)(spark, fileUtils)
		})
	}

	def run(config: Config, workingDir: String): Unit =
		SparkInstance.execute(spark => {
			val fileUtils = new FileUtils()(spark)
			Pipeline.run(config, workingDir)(spark, fileUtils)
		})

}
