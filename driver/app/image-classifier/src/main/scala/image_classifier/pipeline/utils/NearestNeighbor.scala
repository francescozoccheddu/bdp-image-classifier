package image_classifier.pipeline.utils

import image_classifier.pipeline.utils.NearestNeighbor.defaultOutputCol
import org.apache.spark.ml.linalg.{Vector => MLVector}

private[pipeline] final class NearestNeighbor(inputCol: String, outputCol: String = defaultOutputCol) {
	import org.apache.spark.sql.{DataFrame, Dataset, Encoder, Row}

	import reflect.runtime.universe.TypeTag
	import scala.reflect.ClassTag

	def join[T](key: DataFrame, test: DataFrame)(implicit tag: TypeTag[T], classTag: ClassTag[T]): DataFrame =
		join(key, test, inputCol)

	def join[T](key: DataFrame, test: DataFrame, testInputCol: String)(implicit tag: TypeTag[T], classTag: ClassTag[T]): DataFrame =
		join(key, test, (t: DataFrame) => t.rdd.map(_.getAs[T](0)).collect.toSeq, testInputCol)

	def join[T](key: DataFrame, test: DataFrame, map: Row => T)(implicit tag: TypeTag[T], classTag: ClassTag[T]): DataFrame =
		join(key, test, map, inputCol)

	def join[T](key: DataFrame, test: DataFrame, map: Row => T, testInputCol: String)(implicit tag: TypeTag[T], classTag: ClassTag[T]): DataFrame =
		join(key, test, (t: DataFrame) => t.rdd.map(map).collect.toSeq, testInputCol)

	private def join[T](key: DataFrame, test: DataFrame, map: DataFrame => Seq[T], testInputCol: String)(implicit tag: TypeTag[T]): DataFrame = {
		import org.apache.spark.sql.functions.col
		val distinctTest = test.dropDuplicates(testInputCol).cache
		val testData = map(distinctTest)
		val testFeatures = distinctTest.select(col(inputCol)).rdd.map(_.getAs[MLVector](0)).collect
		join(key, testData, testFeatures)
	}

	def join[T](key: DataFrame, test: Dataset[T], map: T => MLVector)(implicit tag: TypeTag[T]): DataFrame =
		join(key, test.collect(), map)

	def join[T](key: DataFrame, test: Seq[T], map: T => MLVector)(implicit tag: TypeTag[T]): DataFrame =
		join(key, test, test.map(map))

	def join[T](key: DataFrame, test: Seq[T], testFeatures: Seq[MLVector])(implicit tag: TypeTag[T]): DataFrame = {
		import org.apache.spark.sql.functions.{col, udf}
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

}

private[pipeline] object NearestNeighbor {

	val defaultOutputCol = "neighbor"

}