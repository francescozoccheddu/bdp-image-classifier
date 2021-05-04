package image_classifier

object Main {
	def main(args: Array[String]): Unit = {
		import org.apache.spark.sql.SparkSession
		import input.Input
		val spark = SparkSession
			.builder()
			.appName("Image classification with BOVW")
			.master("local[*]")
			.getOrCreate()
		try {
			spark.sparkContext.setLogLevel("WARN")
			val configFile = "/home/fra/Desktop/BD/archive/data.json"
			val input = Input.loadFromConfigFile(configFile, spark)
			println(s"I have collected ${input.data.count} images")
		}
		finally
			spark.close
	}
}
