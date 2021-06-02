package org.apache.spark.mllib.clustering

import org.apache.spark.ml.linalg.SQLDataTypes.VectorType
import org.apache.spark.ml.linalg.{Vector => NewVector}
import org.apache.spark.ml.util.SchemaUtils
import org.apache.spark.mllib.clustering.NearestNeighbor.withNorm
import org.apache.spark.mllib.linalg.{Vectors => OldVectors}
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.{col, udf}

final case class NearestNeighbor(codebook: Array[NewVector]) {

	private val codebookWithNorm: Array[VectorWithNorm] = withNorm(codebook)
	private val distanceMeasure: DistanceMeasure = DistanceMeasure.decodeFromString(DistanceMeasure.EUCLIDEAN)
	private val statistics: Array[Double] = distanceMeasure.computeStatistics(codebookWithNorm)

	def join(data: DataFrame, inputCol: String, outputCol: String): DataFrame = {
		SchemaUtils.checkColumnType(data.schema, inputCol, VectorType)
		val predictUdf = udf((vector: NewVector) => predict(vector))
		data.withColumn(outputCol, predictUdf(col(inputCol)))
	}

	def predict(point: NewVector): Int =
		distanceMeasure.findClosest(codebookWithNorm, statistics, withNorm(point))._1

}

object NearestNeighbor {

	def withNorm(vectors: Array[NewVector]): Array[VectorWithNorm] = vectors.map(withNorm)

	def withNorm(vector: NewVector): VectorWithNorm = new VectorWithNorm(OldVectors.fromML(vector))

}