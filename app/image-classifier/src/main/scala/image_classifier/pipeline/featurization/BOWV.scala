package image_classifier.pipeline.featurization

import image_classifier.configuration.CodebookConfig
import image_classifier.pipeline.Columns.{colName, resColName}
import image_classifier.utils.DataTypeImplicits._
import org.apache.spark.ml.clustering.KMeans
import org.apache.spark.ml.linalg.SQLDataTypes.VectorType
import org.apache.spark.ml.linalg.{Vector, Vectors}
import org.apache.spark.mllib.clustering.DistanceMeasureWrapper
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.ArrayType

private[featurization] object BOWV {

	val defaultOutputCol: String = colName("data")
	private val centerCol: String = resColName("center")
	private val distanceCol: String = resColName("distance")
	private val minDistanceCol: String = resColName("minDistance")

	def createCodebook(data: DataFrame, inputCol: String, config: CodebookConfig): Seq[Vector] = {
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
		val assignedCenters = if (config.assignNearest) {
			val centersBroadcast = data.sparkSession.sparkContext.broadcast(centers)
			val distanceUdf = udf((feature: Vector, centerIndex: Int) => Vectors.sqdist(feature, centersBroadcast.value(centerIndex)))
			val window = Window.partitionBy(centerCol)
			val newCenters = model
			  .transform(data)
			  .withColumn(distanceCol, distanceUdf(col(inputCol), col(centerCol)))
			  .withColumn(minDistanceCol, min(distanceCol).over(window))
			  .filter(col(distanceCol) === col(minDistanceCol))
			  .groupBy(centerCol)
			  .agg(first(inputCol).alias(inputCol))
			  .select(inputCol)
			  .collect
			  .map(_.getAs[Vector](0))
			centersBroadcast.destroy
			newCenters
		} else centers
		data.unpersist
		assignedCenters.distinct
	}

	def compute(data: DataFrame, codebook: Seq[Vector], inputCol: String, outputCol: String = defaultOutputCol): DataFrame = {
		data.schema.requireField(inputCol, ArrayType(VectorType))
		val context = data.sparkSession.sparkContext
		val codebookBroadcast = context.broadcast(DistanceMeasureWrapper.withNorm(codebook.toArray))
		val statisticsBroadcast = context.broadcast(DistanceMeasureWrapper.computeStatisticsDistributedly(context, codebookBroadcast))
		val codebookSize = codebook.length
		val nnUdf = udf((features: Array[Vector]) =>
			features.map(f => DistanceMeasureWrapper.findClosestDistributedly(DistanceMeasureWrapper.withNorm(f), statisticsBroadcast, codebookBroadcast))
		)
		val hgUdf = udf((indices: Array[Int]) => Histogram.compute(indices, codebookSize))
		data.withColumn(outputCol, nnUdf(col(inputCol)))
		  .withColumn(outputCol, hgUdf(col(outputCol)))
	}

}