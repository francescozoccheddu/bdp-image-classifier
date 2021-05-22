package image_classifier.pipeline.utils

import image_classifier.pipeline.utils.NearestNeighbor.defaultOutputCol
import org.apache.spark.ml.linalg.{Vector => MLVector}

private[pipeline] final class NearestNeighbor(inputCol: String, outputCol: String = defaultOutputCol) {
	import image_classifier.pipeline.utils.Columns.resColName
	import org.apache.spark.sql.{DataFrame, Row}

	import reflect.runtime.universe.TypeTag

	def naiveJoin[T](key: DataFrame, test: Seq[T], map: T => MLVector)(implicit tag: TypeTag[T]) = {
		import org.apache.spark.sql.functions.{col, udf}
		val testFeatures = test.map(map)
		val mapper = udf[T, MLVector]((f: MLVector) => {
			var minDist: Double = Double.PositiveInfinity
			var minI = 0
			for ((v, i) <- testFeatures.zipWithIndex) {
				import org.apache.spark.ml.linalg.Vectors
				val dist = Vectors.sqdist(v, f)
				if (dist < minDist) {
					minDist = dist
					minI = i
				}
			}
			test(minI)
		})
		key.withColumn(outputCol, mapper(col(inputCol)))
	}

	private val hashCol = resColName("hash")

	def approxJoin[T](key: DataFrame, test: DataFrame, map: Row => T)(implicit tag: TypeTag[T]) = {
		import org.apache.spark.sql.functions.{col, udf}
		import org.apache.spark.ml.feature.BucketedRandomProjectionLSH
		val model = new BucketedRandomProjectionLSH()
			.setBucketLength(3.0)
			.setNumHashTables(5)
			.setInputCol(inputCol)
			.setOutputCol(hashCol)
			.fit(test)
		val mapper = udf((f: MLVector) => map(model.approxNearestNeighbors(test, f, 1).collect.head.asInstanceOf[Row]))
		key.withColumn(outputCol, mapper(col(inputCol)))
	}

}

private[pipeline] object NearestNeighbor {

	val defaultOutputCol = "neighbor"

}