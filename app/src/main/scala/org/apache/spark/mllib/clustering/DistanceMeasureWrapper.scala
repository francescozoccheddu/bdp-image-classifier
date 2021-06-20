package org.apache.spark.mllib.clustering

import org.apache.spark.SparkContext
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.ml.linalg.{Vector => NewVector}
import org.apache.spark.mllib.linalg.{Vectors => OldVectors}

object DistanceMeasureWrapper {

	type DistanceVector = VectorWithNorm

	private val instance: EuclideanDistanceMeasure = new EuclideanDistanceMeasure

	def findClosestDistributedly(point: DistanceVector, statistics: Broadcast[Array[Double]], test: Broadcast[Array[DistanceVector]]): Int =
		instance.findClosest(test.value, statistics.value, point)._1

	def findClosest(point: DistanceVector, statistics: Array[Double], test: Array[DistanceVector]): Int =
		instance.findClosest(test, statistics, point)._1

	def computeStatistics(test: Array[DistanceVector]): Array[Double] =
		instance.computeStatistics(test)

	def computeStatisticsDistributedly(sparkContext: SparkContext, test: Broadcast[Array[DistanceVector]]): Array[Double] =
		instance.computeStatisticsDistributedly(sparkContext, test)

	def withNorm(vectors: Array[NewVector]): Array[DistanceVector] = vectors.map(withNorm)

	def withNorm(vector: NewVector): DistanceVector = new VectorWithNorm(OldVectors.fromML(vector))

}