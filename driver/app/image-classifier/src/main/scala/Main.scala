package com.fra

object Main {
	def main(args: Array[String]): Unit = {
		import org.apache.spark.sql.SparkSession
		val spark = SparkSession
			.builder()
			.appName("Image classification with BOVW")
			.master("local[*]")
			.getOrCreate()
		val configFile = "~/Desktop/BD/archive/data.json"
		val config = InputConfiguration.load(spark.sparkContext, configFile)
		val descriptorFactory = new DescriptorFactory(config.localFeaturesCount)
		val features = spark.sparkContext.union(config.classes.map(_.trainFiles))
		println(features.count())
	}
}
