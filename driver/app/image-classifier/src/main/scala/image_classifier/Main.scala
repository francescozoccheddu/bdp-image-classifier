package image_classifier

import org.apache.spark.sql.SparkSession
import input.Input

object Main {
	
	def main(args: Array[String]): Unit = {
		val configFile = "/home/fra/Desktop/BD/archive/data.json"
		val configDir = java.nio.file.Paths.get(configFile).getParent.toString
		val spark = SparkSession
		.builder()
		.appName("Image classification with BOVW")			
		.master("local[1]")
		.getOrCreate()
		try {
			spark.sparkContext.setLogLevel("WARN")
			val input = Input.loadFromConfigFile(configFile, spark)
			Pipeline.run(input, spark)
		}
		finally
		spark.close
	}
	
}
