package image_classifier

import org.apache.spark.sql.SparkSession
import input.Input

object Main {
	
	def main(args: Array[String]): Unit = {
		val configFile = "/home/fra/Desktop/BD/archive/data.json"
		val configDir = java.nio.file.Paths.get(configFile).getParent.toString
		// TODO Use spark-submit parameters
		val spark = SparkSession
			.builder()
			.appName("Image classification with BOVW")			
			.master("local[*]") 
			.getOrCreate()
		try {
			spark.sparkContext.setLogLevel("WARN")
			// TODO Use S3 or HAR or sequence files on HDFS
			val input = Input.loadFromConfigFile(spark, configFile)
			Pipeline.run(spark, input)
		}
		finally
		spark.close
	}
	
}
