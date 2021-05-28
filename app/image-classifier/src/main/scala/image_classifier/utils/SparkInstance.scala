package image_classifier.utils

import org.apache.log4j.Logger
import org.apache.spark.sql.SparkSession

private[image_classifier] object SparkInstance {

	private val logger: Logger = Logger.getLogger(getClass)

	def execute[T](func: SparkSession => T): T = {
		logger.info("Retrieving Spark session")
		val spark = SparkSession
		  .builder()
		  .appName("Image classification with BOVW")
		  .getOrCreate()
		try {
			func(spark)
		}
		finally {
			spark.close
		}
	}

}
