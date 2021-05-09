package image_classifier

import image_classifier.input.Input
import image_classifier.utils.NearestNeighbor
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession, Row}
import org.apache.spark.sql.functions.{col}
import org.apache.spark.ml.linalg.{Vector => MLVector}

object Pipeline {
	
	val dataCol = "data"
	val isTestCol = "isTest"
	val classCol = "class"
	private val entryCol = "entry"
	
	private def describe(input : Input, spark : SparkSession) : DataFrame = {
		import image_classifier.features.DescriptorFactory
		import org.apache.spark.sql.functions.{udf, monotonically_increasing_id}
		import org.apache.spark.ml.image.ImageSchema
		import spark.implicits._
		val descriptorFactory = DescriptorFactory(input.options.localFeaturesAlgorithm, input.options.localFeaturesCount, input.options.maxImageSize)
		val describeUdf = udf((row : Row) => descriptorFactory.describe(ImageSchema.getWidth(row), ImageSchema.getHeight(row), ImageSchema.getMode(row), ImageSchema.getData(row)))
		val nestedData = input.data.withColumn(dataCol, describeUdf(col(dataCol)))
		// TODO Use flatten() and select
		nestedData
			.withColumn(entryCol, monotonically_increasing_id)
			.flatMap(row => 
				row
				.getAs[Seq[MLVector]](dataCol)
				.map((_, row.getAs[Boolean](isTestCol), row.getAs[Int](classCol), row.getAs[Long](entryCol))))
			.toDF(dataCol, isTestCol, classCol, entryCol)
	}
	
	private def createCodebook(size : Int, data : DataFrame, spark : SparkSession, assignNearest : Boolean = true) : DataFrame = {
		import org.apache.spark.ml.clustering.KMeans
		import org.apache.spark.sql.functions.not
		import spark.implicits._
		val trainSet = data.filter(not(col(isTestCol)))
		val model = new KMeans().setK(size).setMaxIter(200).setFeaturesCol(dataCol).fit(trainSet)
		val centers = model.clusterCenters.toSeq.map(Tuple1(_)).toDF(dataCol)
		if (assignNearest) 
			NearestNeighbor.compute(trainSet, centers, spark).select(col(NearestNeighbor.neighborCol).alias(dataCol))
		else 
			centers
	}
	
	def run(input : Input, spark : SparkSession) {
		val data = describe(input, spark).cache
		val codebook = createCodebook(input.options.codebookSize, data, spark, input.options.codebookAssignNearest).cache
		println("Codebook")
		codebook.show()
	}

}