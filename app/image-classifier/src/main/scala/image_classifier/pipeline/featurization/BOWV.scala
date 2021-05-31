package image_classifier.pipeline.featurization

import image_classifier.configuration.CodebookConfig
import image_classifier.pipeline.Columns.{colName, resColName}
import image_classifier.utils.DataTypeImplicits._
import org.apache.spark.ml.clustering.KMeans
import org.apache.spark.ml.linalg.SQLDataTypes.VectorType
import org.apache.spark.ml.linalg.{Vector, Vectors}
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{ArrayType, IntegerType}

private[featurization] object BOWV {

	val defaultOutputCol: String = colName("data")
	private val codebookDataCol: String = resColName("data")
	private val codebookIdCol: String = resColName("id")
	private val idCol: String = resColName("id")
	private val centerCol: String = resColName("center")
	private val distanceCol: String = resColName("distance")

	def createCodebook(data: DataFrame, inputCol: String, config: CodebookConfig): DataFrame = {
		import data.sparkSession.implicits._
		data.schema.requireField(inputCol, VectorType)
		data.cache
		val model = new KMeans()
		  .setK(config.size)
		  .setMaxIter(config.maxIterations)
		  .setTol(config.convergenceTolerance)
		  .setSeed(config.seed)
		  .setInitSteps(config.initSteps)
		  .setFeaturesCol(inputCol)
		  .setPredictionCol(centerCol)
		  .fit(data)
		val centers = model.clusterCenters
		if (config.assignNearest) {
			val distance = udf((feature: Vector, centerIndex: Int) => Vectors.sqdist(feature, centers(centerIndex)))
			model
			  .transform(data)
			  .withColumn(distanceCol, distance(col(inputCol), col(centerCol)))
			  .groupBy(centerCol, inputCol)
			  .min(distanceCol)
			  .select(col(centerCol).alias(codebookIdCol), col(inputCol).alias(codebookDataCol))
		} else
			centers.distinct.zipWithIndex.toSeq.toDF(codebookDataCol, codebookIdCol)
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