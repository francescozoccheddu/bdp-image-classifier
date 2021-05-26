package image_classifier.utils

private[image_classifier] object SparkInstance {
	import org.apache.spark.sql.SparkSession

	def execute[T](func: SparkSession => T): T = {
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
