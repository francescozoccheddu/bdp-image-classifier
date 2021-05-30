package image_classifier.pipeline.featurization

import image_classifier.pipeline.Columns.{colName, resColName}
import image_classifier.utils.DataTypeImplicits._
import org.apache.spark.ml.clustering.KMeans
import org.apache.spark.ml.linalg.SQLDataTypes.VectorType
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{ArrayType, IntegerType}

private[featurization] object BOWV {

	val defaultOutputCol: String = colName("data")
	private val codebookDataCol: String = resColName("data")
	private val codebookIdCol: String = resColName("id")
	private val idCol: String = resColName("id")

	def createCodebook(data: DataFrame, size: Int, inputCol: String, assignNearest: Boolean): DataFrame = {
		data.schema.requireField(inputCol, VectorType)
		import data.sparkSession.implicits._
		val model = new KMeans().setK(size).setMaxIter(10).setFeaturesCol(inputCol).fit(data)
		val centers = model
		  .clusterCenters
		  .toSeq
		  .zipWithIndex
		  .toDF(codebookDataCol, codebookIdCol)
		val assignedCenters = if (assignNearest)
			new NearestNeighbor(codebookDataCol, codebookDataCol).joinFeatures(centers, data, inputCol)
		else
			centers
		assignedCenters.dropDuplicates(codebookDataCol)
	}

	def compute(data: DataFrame, codebook: DataFrame, codebookSize: Int, inputCol: String, outputCol: String = defaultOutputCol): DataFrame = {
		{
			val dataSchema = data.schema
			dataSchema.requireField(inputCol, ArrayType(VectorType))
			val codebookSchema = codebook.schema
			codebookSchema.requireField(codebookDataCol, VectorType)
			codebookSchema.requireField(codebookIdCol, IntegerType)
		}
		val indexedIn = data.withColumn(idCol, monotonically_increasing_id).cache
		val indexedOut = {
			val explodedData = indexedIn.select(
				col(idCol),
				explode(col(inputCol)).alias(inputCol))
			val vectorizeUdf = udf((matches: Array[Long]) => Histogram.compute(matches, codebookSize))
			new NearestNeighbor(inputCol, outputCol)
			  .join[Int](explodedData, codebook, codebookIdCol, codebookDataCol)
			  .select(col(idCol), col(outputCol))
			  .groupBy(col(idCol))
			  .agg(collect_list(outputCol).alias(outputCol))
			  .withColumn(outputCol, vectorizeUdf(col(outputCol)))
		}
		val joint = indexedIn.drop(outputCol).join(indexedOut, idCol).drop(idCol)
		joint
	}

}