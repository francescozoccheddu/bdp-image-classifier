package image_classifier.utils

import image_classifier.Pipeline.dataCol
import org.apache.spark.sql.{SparkSession, DataFrame}
import org.apache.spark.ml.linalg.{Vector => MLVector}

object NearestNeighbor {

	private val hashCol = "hash"
	private val distCol = "distance"
	val neighborCol = "neighbor"

	def compute(test : DataFrame, key : DataFrame, spark : SparkSession) : DataFrame = {

		import org.apache.spark.ml.feature.BucketedRandomProjectionLSH
		import org.apache.spark.ml.linalg.Vectors
		import org.apache.spark.sql.functions.{col, udf}
		
		val model = new BucketedRandomProjectionLSH()
			.setBucketLength(2.0)
			.setNumHashTables(3)
			.setInputCol(dataCol)
			.setOutputCol(hashCol)
			.fit(test)

		val transTest = model.transform(test)
		val neighborUdf = udf((data : MLVector) => model.approxNearestNeighbors(transTest, data, 1, distCol).drop(distCol).first)
		key.withColumn(neighborCol, neighborUdf(col(dataCol)))
		
	}

}