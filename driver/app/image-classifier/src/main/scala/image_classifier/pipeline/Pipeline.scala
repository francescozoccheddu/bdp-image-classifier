package image_classifier.pipeline

object Pipeline {
	import image_classifier.configuration.Config
	import org.apache.spark.sql.SparkSession

	def run(config: Config, workingDir: String)(implicit spark: SparkSession): Unit = {
	}

}
