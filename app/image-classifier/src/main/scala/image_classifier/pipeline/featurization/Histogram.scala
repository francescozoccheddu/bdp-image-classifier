package image_classifier.pipeline.featurization

import org.apache.spark.ml.linalg.{Vector, Vectors}

private[featurization] object Histogram {

	def compute(data: Seq[Int], codebookSize: Int): Vector = {
		val minDensityForComputation = 0.5
		val minDensity = 0.7
		val minSparseSize = 5
		if (codebookSize > minSparseSize) {
			val maxDensity = data.length.toDouble / codebookSize
			if (maxDensity <= minDensityForComputation)
				computeSparse(data, codebookSize)
			else {
				val dense = computeDense(data, codebookSize)
				val density = dense.numNonzeros.toDouble / codebookSize
				if (density >= minDensity) dense else dense.toSparse
			}
		}
		else computeDense(data, codebookSize)
	}

	def computeSparse(data: Seq[Int], codebookSize: Int): Vector = {
		val map = scala.collection.mutable.Map[Int, Double]().withDefaultValue(0)
		for (n <- data) map(n) += 1
		val entries = map
		  .toSeq
		  .sortBy(_._1)
		  .map { case (i, v) => (i, v / data.length.toDouble) }
		Vectors.sparse(codebookSize, entries)
	}

	def computeDense(data: TraversableOnce[Int], codebookSize: Int): Vector = {
		val bins = Array.ofDim[Double](codebookSize)
		var sum = 0
		for (n <- data) {
			sum += 1
			bins(n) += 1
		}
		for (i <- 0 until codebookSize) {
			bins(i) /= sum.toDouble
		}
		Vectors.dense(bins)
	}

}