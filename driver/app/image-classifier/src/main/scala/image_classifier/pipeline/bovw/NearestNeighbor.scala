package image_classifier.pipeline.bovw

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.col

private[pipeline] object NearestNeighbor {

	def join(test: DataFrame, key: DataFrame, dataColName: String, neighborColName: String): DataFrame =
		join(test, key, test.schema.fieldNames intersect key.schema.fieldNames, dataColName, neighborColName)

	def join(test: DataFrame, key: DataFrame, cols: Seq[String], dataColName: String, neighborColName: String): DataFrame = {

		import org.apache.spark.ml.knn.KNN
		import org.apache.spark.sql.functions.explode

		val testSize = test.count()
		val topTreeSize = math.min(math.max(testSize / 200, 2), testSize).toInt

		val model = new KNN()
			.setFeaturesCol(dataColName)
			.setAuxCols(cols.toArray)
			.setTopTreeSize(topTreeSize)
			.setK(1)
			.fit(test)
			.setNeighborsCol(neighborColName)

		model
			.transform(key)
			.withColumn(neighborColName, explode(col(neighborColName)))

	}

}