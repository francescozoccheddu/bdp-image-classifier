package image_classifier.pipeline.featurization

import image_classifier.pipeline.featurization.NearestNeighbor.defaultOutputCol
import org.apache.spark.ml.linalg.{Vector => MLVector}

private[featurization] final class NearestNeighbor(inputCol: String, testInputCol: String, outputCol: String = defaultOutputCol) {
	import org.apache.spark.sql.functions.col
	import org.apache.spark.sql.{DataFrame, Dataset, Encoder, Row}

	import scala.reflect.ClassTag
	import scala.reflect.runtime.universe.TypeTag

	def this(inputCol: String, outputCol: String) = this(inputCol, inputCol, outputCol)
	def this(inputCol: String) = this(inputCol, inputCol, defaultOutputCol)

	def joinFeatures(key: DataFrame, test: DataFrame): DataFrame = {
		import org.apache.spark.sql.functions.col
		val testFeatures = test
			.dropDuplicates(testInputCol)
			.select(col(testInputCol))
			.rdd
			.map(_.getAs[MLVector](0))
			.collect
		join(key, testFeatures, testFeatures)
	}

	def joinColumn[T](key: DataFrame, test: DataFrame, column: String)(implicit tag: TypeTag[T], classTag: ClassTag[T]): DataFrame = {
		if (column == testInputCol)
			joinFeatures(key, test)
		else {
			val index = test.schema.fieldIndex(column)
			joinMap(key, test, (r: Row) => r.getAs[T](index))
		}
	}

	def joinAs[T](key: DataFrame, test: DataFrame)(implicit tag: TypeTag[T], encoder: Encoder[T]): DataFrame =
		joinMap[T](key, test, (t: DataFrame) => t.as[T].collect.toSeq)

	def joinMap[T](key: DataFrame, test: DataFrame, map: Row => T)(implicit tag: TypeTag[T], classTag: ClassTag[T]): DataFrame =
		joinMap[T](key, test, (t: DataFrame) => t.rdd.map(map).collect.toSeq)

	private def joinMap[T](key: DataFrame, test: DataFrame, map: DataFrame => Seq[T])(implicit tag: TypeTag[T]): DataFrame = {
		val distinctTest = test.dropDuplicates(testInputCol).cache
		val testData = map(distinctTest)
		val testFeatures = distinctTest.select(col(testInputCol)).rdd.map(_.getAs[MLVector](0)).collect
		join(key, testData, testFeatures)
	}

	def join[T](key: DataFrame, test: Dataset[T])(implicit tag: TypeTag[T]): DataFrame =
		join(key, test.collect(), test.select(col(testInputCol)).rdd.map(_.getAs[MLVector](0)).collect)

	def join[T](key: DataFrame, test: Seq[T], featureProvider: T => MLVector)(implicit tag: TypeTag[T]): DataFrame =
		join(key, test, test.map(featureProvider))

	def join[T](key: DataFrame, test: Seq[T], testFeatures: Seq[MLVector])(implicit tag: TypeTag[T]): DataFrame = {
		import org.apache.spark.sql.functions.udf
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