package image_classifier.pipeline.featurization

import image_classifier.configuration.CodebookConfig
import image_classifier.pipeline.utils.Columns.{colName, resColName}
import image_classifier.pipeline.utils.DataTypeImplicits._
import image_classifier.pipeline.utils.SampleUtils
import org.apache.log4j.Logger
import org.apache.spark.ml.clustering.KMeans
import org.apache.spark.ml.linalg.SQLDataTypes.VectorType
import org.apache.spark.ml.linalg.{Vector, Vectors}
import org.apache.spark.mllib.clustering.DistanceMeasureWrapper
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{ArrayType, IntegerType}

private[featurization] object BOWV {

	val defaultOutputCol: String = colName("data")
	private val centerCol: String = resColName("center")
	private val distanceCol: String = resColName("distance")
	private val minDistanceCol: String = resColName("minDistance")
	private val logger: Logger = Logger.getLogger(getClass)

	def createCodebook(data: DataFrame, inputCol: String, labelCol: String, config: CodebookConfig): Seq[Vector] = {
		{
			val schema = data.schema
			schema.requireField(inputCol, ArrayType(VectorType))
			schema.requireField(labelCol, IntegerType)
		}
		val projData = {
			val selectedData = data.select(inputCol, labelCol)
			val imageSample = SampleUtils
			  .sample(selectedData, labelCol, config.imageSample)
			  .select(explode(col(inputCol)).alias(inputCol), col(labelCol))
			SampleUtils
			  .sample(imageSample, labelCol, config.featureSample)
			  .drop(labelCol)
		}.cache
		logger.info(s"Clustering with K=${config.size}, max ${config.maxIterations} iterations, max ${config.initSteps} steps")
		val model = new KMeans()
		  .setK(config.size)
		  .setMaxIter(config.maxIterations)
		  .setTol(config.convergenceTolerance)
		  .setSeed(config.seed)
		  .setInitSteps(config.initSteps)
		  .setFeaturesCol(inputCol)
		  .setPredictionCol(centerCol)
		  .fit(projData)
		val centers = model.clusterCenters
		val assignedCenters = if (config.assignNearest) {
			logger.info("Assigning nearest features")
			val centersBroadcast = projData.sparkSession.sparkContext.broadcast(centers)
			val distanceUdf = udf((feature: Vector, centerIndex: Int) => Vectors.sqdist(feature, centersBroadcast.value(centerIndex)))
			val window = Window.partitionBy(centerCol)
			val newCenters = model
			  .transform(projData)
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
		projData.unpersist
		assignedCenters.distinct
	}

	def compute(data: DataFrame, codebook: Seq[Vector], inputCol: String, outputCol: String = defaultOutputCol): DataFrame = {
		data.schema.requireField(inputCol, ArrayType(VectorType))
		logger.info("Computing statistics")
		val context = data.sparkSession.sparkContext
		val codebookBroadcast = context.broadcast(DistanceMeasureWrapper.withNorm(codebook.toArray))
		val statisticsBroadcast = context.broadcast(DistanceMeasureWrapper.computeStatisticsDistributedly(context, codebookBroadcast))
		val codebookSize = codebook.length
		val nnUdf = udf((features: Array[Vector]) =>
			features.map(f => DistanceMeasureWrapper.findClosestDistributedly(DistanceMeasureWrapper.withNorm(f), statisticsBroadcast, codebookBroadcast))
		)
		val hgUdf = udf((indices: Array[Int]) => Histogram.compute(indices, codebookSize))
		logger.info("Computing nearest neighbors")
		data.withColumn(outputCol, nnUdf(col(inputCol)))
		  .withColumn(outputCol, hgUdf(col(outputCol)))
	}

}