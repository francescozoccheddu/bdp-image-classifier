package image_classifier.utils

import image_classifier.Pipeline.dataCol
import org.apache.spark.sql.{SparkSession, DataFrame}
import org.apache.spark.ml.linalg.{Vector => MLVector}

object NearestNeighbor {

	val neighborCol = "neighbor"

	def join(test : DataFrame, key : DataFrame, spark : SparkSession) : DataFrame = 
		join(test, key, test.schema.fieldNames.toSeq, spark)
		
	def join(test : DataFrame, key : DataFrame, auxCols : Seq[String], spark : SparkSession) : DataFrame = {

		import org.apache.spark.ml.knn.KNN

		// TODO Calculate TopTreeSize

		val model = new KNN()
			.setFeaturesCol(dataCol)
			.setAuxCols(auxCols.toArray)
			.setTopTreeSize(math.max((test.count() / 500).toInt, 2))
			.setK(1)
			.fit(test)

		model.transform(key).withColumnRenamed(model.getNeighborsCol, neighborCol)

		println("A join B")
		predicted.show()
		
	}

}