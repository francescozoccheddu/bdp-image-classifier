package image_classifier.pipeline.featurization

import scala.reflect.runtime.universe.TypeTag
import image_classifier.pipeline.featurization.NearestNeighbor.defaultOutputCol
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.ml.linalg.{Vectors, Vector => MLVector}
import org.apache.spark.sql.functions.{col, udf}
import org.apache.spark.sql.{DataFrame, Row}

private[featurization] final class NearestNeighbor(inputCol: String, outputCol: String = defaultOutputCol) {

	def join[T](key: DataFrame, test: DataFrame, neighborCol: String, testInputCol: String)(implicit tag: TypeTag[T]): DataFrame = {
		if (testInputCol == neighborCol)
			joinFeatures(key, test, testInputCol)
		else
			join(key, test.select(neighborCol, testInputCol).collect(), (r: Row) => r.getAs[T](0), (r: Row) => r.getAs[MLVector](1))
	}

	def joinFeatures(key: DataFrame, test: DataFrame, testInputCol: String): DataFrame = {
		val testFeatures = test.select(testInputCol).collect().map(_.getAs[MLVector](0))
		join(key, testFeatures, testFeatures)
	}

	def join[T](key: DataFrame, test: DataFrame, neighborProvider: Row => T, testInputCol: String)(implicit tag: TypeTag[T]): DataFrame = {
		val schema = test.schema
		val inputIndex = schema.fieldIndex(testInputCol)
		join(key, test.collect(), neighborProvider, (r: Row) => r.getAs[MLVector](inputIndex))
	}

	def join[T, N](key: DataFrame, test: Seq[T], neighborProvider: T => N, featureProvider: T => MLVector)(implicit tag: TypeTag[N]): DataFrame =
		join(key, test.map(neighborProvider), test.map(featureProvider))

	def join[T](key: DataFrame, test: Seq[T], featureProvider: T => MLVector)(implicit tag: TypeTag[T]): DataFrame =
		join(key, test, test.map(featureProvider))

	def join[T](key: DataFrame, test: Seq[T], testFeatures: Seq[MLVector])(implicit tag: TypeTag[T]): DataFrame = {
		val spark = key.sparkSession.sparkContext
		val testBroadcast = spark.broadcast(test)
		val testFeaturesBroadcast = if (test == testFeatures) testBroadcast.asInstanceOf[Broadcast[Seq[MLVector]]] else spark.broadcast(testFeatures)
		val mapper = udf[T, MLVector]((f: MLVector) => {
			var minDist: Double = Double.PositiveInfinity
			var minI = 0
			for ((v, i) <- testFeaturesBroadcast.value.zipWithIndex) {
				val dist = Vectors.sqdist(v, f)
				if (dist < minDist) {
					minDist = dist
					minI = i
				}
			}
			testBroadcast.value(minI)
		})
		key.withColumn(outputCol, mapper(col(inputCol)))
	}

}

private[pipeline] object NearestNeighbor {

	val defaultOutputCol: String = "neighbor"

}