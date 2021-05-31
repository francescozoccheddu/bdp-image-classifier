package image_classifier.pipeline.featurization

import image_classifier.configuration.CodebookConfig
import image_classifier.pipeline.Columns.{colName, resColName}
import image_classifier.utils.DataTypeImplicits._
import org.apache.spark.ml.clustering.KMeans
import org.apache.spark.ml.linalg.SQLDataTypes.VectorType
import org.apache.spark.ml.linalg.{Vector, Vectors}
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.ArrayType

private[featurization] object BOWV {

	val defaultOutputCol: String = colName("data")
	private val centerCol: String = resColName("center")
	private val distanceCol: String = resColName("distance")

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
			val distance = udf((feature: Vector, centerIndex: Int) => Vectors.sqdist(feature, centersBroadcast.value(centerIndex)))
			val newCenters = model
			  .transform(data)
			  .withColumn(distanceCol, distance(col(inputCol), col(centerCol)))
			  .groupBy(centerCol, inputCol)
			  .min(distanceCol)
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
		val codebookBroadcast = data.sparkSession.sparkContext.broadcast(codebook)
		val featurize = udf((features: Seq[Vector]) => {
			val neighbors = features.map(f => {
				var minDist = Double.PositiveInfinity
				var minI = 0
				var i = codebookBroadcast.value.length
				while (i > 0) {
					i -= 1
					val dist = Vectors.sqdist(f, codebookBroadcast.value(i))
					if (dist < minDist) {
						minDist = dist
						minI = i
					}
				}
				minI
			})
			Histogram.compute(neighbors, codebookBroadcast.value.length)
		})
		data.withColumn(outputCol, featurize(col(inputCol)))
	}

}