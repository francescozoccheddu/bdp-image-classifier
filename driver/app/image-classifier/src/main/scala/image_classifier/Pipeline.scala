package image_classifier

import image_classifier.input.Input
import image_classifier.utils.NearestNeighbor
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession, Row}
import org.apache.spark.sql.functions.{col}
import org.apache.spark.ml.linalg.{Vector => MLVector}

object Pipeline {
	
	val dataColName = "data"
	val isTestColName = "isTest"
	val classColName = "class"
	val neighborColName = "neighbor"
	private val entryColName = "entry"
	
	val dataCol = col(dataColName)
	val isTestCol = col(isTestColName)
	val classCol = col(classColName)
	val neighborCol = col(neighborColName)
	private val entryCol = col(entryColName)
	
	private def describe(spark : SparkSession, input : Input) : DataFrame = {
		import image_classifier.features.DescriptorFactory
		import org.apache.spark.sql.functions.{udf, monotonically_increasing_id, explode}
		import org.apache.spark.ml.image.ImageSchema
		import spark.implicits._
		val descriptorFactory = DescriptorFactory(input.options.localFeaturesCount, input.options.maxImageSize)
		val describeUdf = udf((row : Row) => descriptorFactory.describe(ImageSchema.getWidth(row), ImageSchema.getHeight(row), ImageSchema.getMode(row), ImageSchema.getData(row)))
		input.data
			.withColumn(entryColName, monotonically_increasing_id)
			.withColumn(dataColName, explode(describeUdf(dataCol)))
	}
	
	private def createCodebook(spark : SparkSession, size : Int, data : DataFrame, assignNearest : Boolean = true) : DataFrame = {
		import org.apache.spark.ml.clustering.KMeans
		import org.apache.spark.sql.functions.{monotonically_increasing_id, explode}
		import spark.implicits._
		val trainSet = data.filter(!isTestCol)
		val model = new KMeans().setK(size).setMaxIter(200).setFeaturesCol(dataColName).fit(trainSet)
		val centers = model.clusterCenters.toSeq.map(Tuple1(_)).toDF(dataColName)
		if (assignNearest) 
			NearestNeighbor.join(spark, trainSet, centers, Seq(dataColName))
				.select(neighborCol.getField(dataColName).alias(dataColName))
		else 
			centers
	}
	
	private def computeBOFS(spark : SparkSession, data : DataFrame, codebook : DataFrame) : DataFrame = {
		val matches = {
			import org.apache.spark.sql.functions.monotonically_increasing_id
			val indexedCodebook = codebook.withColumn(entryColName, monotonically_increasing_id)
			val joint = NearestNeighbor.join(spark, indexedCodebook, data, Seq(entryColName))
				.select(entryCol, neighborCol.getField(entryColName).alias(neighborColName))
			joint.join(data.drop(dataCol), entryColName)
		}
		???
	}

	def run(spark : SparkSession,input : Input) {
		val data = describe(spark, input).cache
		val codebook = createCodebook(spark, input.options.codebookSize, data, input.options.codebookAssignNearest).cache
		val bofs = computeBOFS(spark, data, codebook)
	}

}