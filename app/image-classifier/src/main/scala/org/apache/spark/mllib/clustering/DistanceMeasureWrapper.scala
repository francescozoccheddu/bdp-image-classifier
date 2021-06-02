package org.apache.spark.mllib.clustering

import org.apache.spark.SparkContext
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.ml.linalg.{Vector => NewVector}
import org.apache.spark.mllib.linalg.{Vectors => OldVectors}

object DistanceMeasureWrapper {

	private val instance: EuclideanDistanceMeasure = new EuclideanDistanceMeasure

	def findClosestDistributedly(point: VectorWithNorm, statistics: Broadcast[Array[Double]], test: Broadcast[Array[VectorWithNorm]]): Int =
		instance.findClosest(test.value, statistics.value, point)._1

	def findClosest(point: VectorWithNorm, statistics: Array[Double], test: Array[VectorWithNorm]): Int =
		instance.findClosest(test, statistics, point)._1

	def computeStatistics(test: Array[VectorWithNorm]): Array[Double] =
		instance.computeStatistics(test)

	def computeStatisticsDistributedly(sparkContext: SparkContext, test: Broadcast[Array[VectorWithNorm]]): Array[Double] =
		instance.computeStatisticsDistributedly(sparkContext, test)

	def withNorm(vectors: Array[NewVector]): Array[VectorWithNorm] = vectors.map(withNorm)

	def withNorm(vector: NewVector): VectorWithNorm = new VectorWithNorm(OldVectors.fromML(vector))

}