package image_classifier.utils

import org.apache.spark.sql.{SparkSession, DataFrame}
import org.apache.spark.sql.functions.col

object NearestNeighbor {
	
	val neighborColName = "neighbor"
	val neighborCol = col(neighborColName)
	
	def join(spark : SparkSession, test : DataFrame, key : DataFrame) : DataFrame = 
	join(spark, test, key, test.schema.fieldNames.toSeq)
	
	def join(spark : SparkSession, test : DataFrame, key : DataFrame, auxCols : Seq[String]) : DataFrame = {
		
		import image_classifier.Pipeline.dataColName
		import org.apache.spark.sql.functions.explode
		import org.apache.spark.ml.knn.KNN
		import spark.implicits._

		val topTreeSize = math.max((test.count() / 500).toInt, 2)

		val model = new KNN()
			.setFeaturesCol(dataColName)
			.setAuxCols(auxCols.toArray)
			.setTopTreeSize(topTreeSize)
			.setK(1)
			.fit(test)

		model
			.transform(key)
			.withColumnRenamed(model.getNeighborsCol, neighborColName)
			.withColumn(neighborColName, explode(neighborCol))
		
	}

}