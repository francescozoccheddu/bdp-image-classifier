package image_classifier

import org.apache.spark.sql.functions.col
import image_classifier.features.ImageReader
import org.apache.spark.ml.image.ImageSchema
import org.bytedeco.javacpp.opencv_highgui.{imshow, waitKey}
import org.apache.spark.sql.Row
import image_classifier.features.DescriptorFactory

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
			val descriptorFactory = DescriptorFactory(input.options.localFeaturesAlgorithm, input.options.localFeaturesCount, input.options.maxImageSize)
			val row = input.data.first().getAs[Row]("image")
			val mat = descriptorFactory.describe(ImageSchema.getWidth(row), ImageSchema.getHeight(row), ImageSchema.getMode(row), ImageSchema.getData(row))
			println(mat.mkString("\n"))
		}
		finally
			spark.close
	}
}
