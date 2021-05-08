package image_classifier

import image_classifier.input.Input
import image_classifier.features.DescriptorFactory
import org.apache.spark.sql.SparkSession
import org.apache.spark.ml.image.ImageSchema
import org.bytedeco.javacpp.opencv_highgui.{imshow, waitKey}
import org.apache.spark.sql.{DataFrame, SparkSession, Row}
import org.apache.spark.sql.functions.{col, udf, monotonically_increasing_id}
import org.apache.spark.mllib.linalg.{Vector => MLVector}

object Pipeline {
	
	val dataCol = "data"
	val isTestCol = "isTest"
	val classCol = "class"
	private val entryCol = "entry"
	
	private def describe(input : Input, spark : SparkSession) : DataFrame = {
		import spark.implicits._
		val descriptorFactory = DescriptorFactory(input.options.localFeaturesAlgorithm, input.options.localFeaturesCount, input.options.maxImageSize)
		val describeUdf = udf((row : Row) => descriptorFactory.describe(ImageSchema.getWidth(row), ImageSchema.getHeight(row), ImageSchema.getMode(row), ImageSchema.getData(row)))
		val nestedData = input.data.withColumn(dataCol, describeUdf(input.data.col(dataCol)))
		println("--- NESTED DATA ---")
		nestedData.show()
		nestedData
			.withColumn(entryCol, monotonically_increasing_id())
			.flatMap(row => 
				row
				.getAs[Seq[MLVector]](dataCol)
				.map((_, row.getAs[Boolean](isTestCol), row.getAs[Int](classCol), row.getAs[Long](entryCol))))
			.toDF(dataCol, isTestCol, classCol, entryCol)
	}
		
	def run(input : Input, spark : SparkSession) {
		val data = describe(input, spark)
		println("--- DATA ---")
		data.show()
	}

}