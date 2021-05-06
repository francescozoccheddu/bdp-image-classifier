package image_classifier

import org.apache.spark.sql.functions.col

object Main {
	def main(args: Array[String]): Unit = {
		import org.apache.spark.sql.SparkSession
		import input.Input
		val configFile = "/home/fra/Desktop/BD/archive/data.json"
		val configDir = java.nio.file.Paths.get(configFile).getParent.toString
		val spark = SparkSession
			.builder()
			.appName("Image classification with BOVW")
			.master("local[*]")
			.getOrCreate()
		try {
			spark.sparkContext.setLogLevel("WARN")
			val input = Input.loadFromConfigFile(configFile, spark)
			val image = input.data.select(col(Input.imageCol)).first.get(0)
			println(image)
		}
		finally
			spark.close
	}
}
