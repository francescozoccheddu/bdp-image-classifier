package image_classifier.pipeline.featurization

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.col

private[featurization] object NearestNeighbor {

	def join(test: DataFrame, key: DataFrame, dataColName: String, neighborColName: String): DataFrame =
		join(test, key, test.schema.fieldNames intersect key.schema.fieldNames, dataColName, neighborColName)

	def join(test: DataFrame, key: DataFrame, cols: Seq[String], dataColName: String, neighborColName: String): DataFrame = {

		import org.apache.spark.ml.knn.KNN
		import org.apache.spark.sql.functions.explode

		val model = new KNN()
			.setFeaturesCol(dataColName)
			.setAuxCols(cols.toArray)
			.setTopTreeSizeHint(test.count)
			.fit(test)
			.setNeighborCol(neighborColName)

		model.transform(key)

	}

}