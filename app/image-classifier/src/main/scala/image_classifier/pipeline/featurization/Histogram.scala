package image_classifier.pipeline.featurization

import org.apache.spark.ml.linalg.{Vectors, Vector => MLVector}

private[featurization] object Histogram {

	def compute(data: Seq[Long], codebookSize: Int): MLVector = {
		val sparseFraction = 0.1
		val minSparseSize = 100
		if (data.length > minSparseSize && data.length < codebookSize.toDouble * sparseFraction)
			computeSparse(data, codebookSize)
		else
			computeDense(data, codebookSize)
	}

	def computeSparse(data: Seq[Long], codebookSize: Int): MLVector = {
		val map = scala.collection.mutable.Map[Int, Double]().withDefaultValue(0)
		for (n <- data)
			map(n.toInt) += 1
		val entries = map
		  .toSeq
		  .sortBy(_._1)
		  .map { case (i, v) => (i, v / data.length.toDouble) }
		Vectors.sparse(codebookSize, entries)
	}

	def computeDense(data: TraversableOnce[Long], codebookSize: Int): MLVector = {
		val bins = Array.ofDim[Double](codebookSize)
		var sum: Long = 0
		for (n <- data) {
			sum += 1
			bins(n.toInt) += 1
		}
		for (i <- 0 until codebookSize) {
			bins(i) /= sum.toDouble
		}
		Vectors.dense(bins)
	}

}