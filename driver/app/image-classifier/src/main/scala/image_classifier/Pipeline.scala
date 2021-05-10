package image_classifier

import image_classifier.input.Input
import image_classifier.utils.NearestNeighbor
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession, Row}
import org.apache.spark.sql.functions.{col}
import org.apache.spark.ml.linalg.{Vector => MLVector}
import image_classifier.utils.DataFrameImplicits._

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
	
	private def describe(spark : SparkSession, data : DataFrame, localFeaturesCount : Int, maxImageSize : Int) : DataFrame = {
		import image_classifier.features.DescriptorFactory
		import org.apache.spark.sql.functions.{udf, monotonically_increasing_id, explode}
		import org.apache.spark.ml.image.ImageSchema
		import spark.implicits._
		val descriptorFactory = DescriptorFactory(localFeaturesCount, maxImageSize)
		val describeUdf = udf((row : Row) => descriptorFactory.describe(ImageSchema.getWidth(row), ImageSchema.getHeight(row), ImageSchema.getMode(row), ImageSchema.getData(row)))
		data.withColumn(dataColName, explode(describeUdf(dataCol)))
	}
	
	private def createCodebook(spark : SparkSession, size : Int, data : DataFrame, assignNearest : Boolean = true) : DataFrame = {
		import org.apache.spark.ml.clustering.KMeans
		import org.apache.spark.sql.functions.explode
		import spark.implicits._
		val model = new KMeans().setK(size).setMaxIter(200).setFeaturesCol(dataColName).fit(data)
		val centers = model.clusterCenters.toSeq.map(Tuple1(_)).toDF(dataColName)
		if (assignNearest) 
			NearestNeighbor.join(spark, data, centers, Seq(dataColName))
				.select(neighborCol.getField(dataColName).alias(dataColName))
		else 
			centers
	}
	
	private def matchFeatures(spark : SparkSession, data : DataFrame, codebook : DataFrame) : DataFrame = {
		val indexedCodebook = {
			import org.apache.spark.sql.functions.{monotonically_increasing_id, row_number, lit}
			import org.apache.spark.sql.expressions.Window
			val window = Window.orderBy(monotonically_increasing_id).partitionBy(lit(0))
			codebook.withColumn(entryColName, row_number.over(window).cast("long"))
		}
		NearestNeighbor.join(spark, indexedCodebook, data, Seq(entryColName))
			.select(entryCol, neighborCol.getField(entryColName).alias(neighborColName))
	}


	def run(spark : SparkSession,input : Input) {
		val (legend, matches) = {
			val indexedInput = {
				import org.apache.spark.sql.functions.monotonically_increasing_id
				input.data.withColumn(entryColName, monotonically_increasing_id)
			} 
			val features = describe(spark, indexedInput, input.options.localFeaturesCount, input.options.maxImageSize).cache
			val codebook = createCodebook(spark, input.options.codebookSize, features.filter(!isTestCol), input.options.codebookAssignNearest).cache
			val matches = matchFeatures(spark, features, codebook).cache
			codebook.unpersist
			val legend = features.select(entryCol, isTestCol, classCol).cache
			features.unpersist
			(legend, matches)
		} 
		legend.print("Legend")
		matches.print("Matches")
	}

}