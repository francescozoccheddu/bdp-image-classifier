package image_classifier.utils

import image_classifier.Pipeline
import org.apache.spark.sql.{SparkSession, DataFrame}
import org.apache.spark.sql.functions.col

object NearestNeighbor {
	
	val neighborColName = "neighbor"
	val neighborCol = col(neighborColName)
	
	def join(spark : SparkSession, test : DataFrame, key : DataFrame) : DataFrame = 
		join(spark, test, key, Pipeline.dataColName)

	def join(spark : SparkSession, test : DataFrame, key : DataFrame, dataColName : String) : DataFrame = 
		join(spark, test, key, test.schema.fieldNames intersect key.schema.fieldNames, dataColName)
	
	def join(spark : SparkSession, test : DataFrame, key : DataFrame, cols : Seq[String], dataColName : String = Pipeline.dataColName) : DataFrame = {
		
		import org.apache.spark.sql.functions.explode
		import org.apache.spark.ml.knn.KNN
		import spark.implicits._

		val testSize = test.count()
		val topTreeSize = math.min(math.max(testSize / 200, 2), testSize).toInt

		val model = new KNN()
			.setFeaturesCol(dataColName)
			.setAuxCols(cols.toArray)
			.setTopTreeSize(topTreeSize)
			.setK(1)
			.fit(test)

		model
			.transform(key)
			.withColumnRenamed(model.getNeighborsCol, neighborColName)
			.withColumn(neighborColName, explode(neighborCol))
		
	}

}