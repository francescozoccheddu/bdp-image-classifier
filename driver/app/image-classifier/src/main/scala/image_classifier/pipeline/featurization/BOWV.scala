package image_classifier.pipeline.featurization

import org.apache.spark.sql.DataFrame

private[featurization] object BOWV {

	val defaultOutputDataCol = "data"
	val defaultOutputIdCol = "id"
	val defaultTempCol = "BOFsUtils_internal_id"

	def createCodebook(data: DataFrame, size: Int, inputDataCol: String): DataFrame =
		createCodebook(data, size, inputDataCol, true)

	def createCodebook(data: DataFrame, size: Int, inputDataCol: String, assignNearest: Boolean): DataFrame =
		createCodebook(data, size, inputDataCol, defaultOutputDataCol, defaultOutputIdCol, assignNearest)

	def createCodebook(data: DataFrame, size: Int, inputDataCol: String, outputDataCol: String, outputIdCol: String, assignNearest: Boolean = true): DataFrame = {
		{
			import image_classifier.utils.DataTypeImplicits._
			import org.apache.spark.ml.linalg.SQLDataTypes.VectorType
			data.schema.requireField(inputDataCol, VectorType)
		}
		import data.sparkSession.implicits._
		import org.apache.spark.ml.clustering.KMeans
		import org.apache.spark.sql.functions.col
		val model = new KMeans().setK(size).setMaxIter(200).setFeaturesCol(inputDataCol).fit(data)
		val centers = model.clusterCenters.toSeq.zipWithIndex.toDF(outputDataCol, outputIdCol)
		if (assignNearest) {
			import org.apache.spark.sql.types.IntegerType
			val dataWithId = {
				val field = data.schema.fields.find(_.dataType == IntegerType)
				if (field.isDefined) {
					data.withColumnRenamed(field.get.name, outputIdCol)
				}
				else {
					import org.apache.spark.sql.functions.lit
					data.withColumn(outputIdCol, lit(0))
				}
			}.select(col(outputIdCol), col(inputDataCol).alias(outputDataCol))
			NearestNeighbor.join(dataWithId, centers, Seq(outputDataCol, outputIdCol), outputDataCol, outputDataCol)
				.select(col(outputDataCol).getField(outputDataCol).alias(outputDataCol), col(outputDataCol).getField(outputIdCol).alias(outputIdCol))
		}
		else
			centers
	}

	def compute(data: DataFrame, codebook: DataFrame, codebookSize: Int, inputDataCol: String): DataFrame =
		compute(data, codebook, codebookSize, inputDataCol, defaultOutputDataCol, defaultOutputIdCol, defaultOutputDataCol, defaultTempCol)

	def compute(data: DataFrame, codebook: DataFrame, codebookSize: Int, inputDataCol: String, codebookDataCol: String, codebookIdCol: String, outputDataCol: String, tempCol: String): DataFrame = {
		import org.apache.spark.sql.types.IntegerType
		{
			import image_classifier.utils.DataTypeImplicits._
			import org.apache.spark.ml.linalg.SQLDataTypes.VectorType
			import org.apache.spark.sql.types.ArrayType
			val dataSchema = data.schema
			dataSchema.requireField(inputDataCol, ArrayType(VectorType))
			dataSchema.requireNoField(tempCol)
			val codebookSchema = codebook.schema
			codebookSchema.requireField(codebookDataCol, VectorType)
			codebookSchema.requireField(codebookIdCol, IntegerType)
			require(outputDataCol != tempCol)
			require(codebookIdCol != tempCol)
			require(codebookDataCol != tempCol)
		}
		val indexedIn = {
			import org.apache.spark.sql.functions.monotonically_increasing_id
			data.withColumn(tempCol, monotonically_increasing_id.cast(IntegerType)).cache
		}
		val indexedOut = {
			import org.apache.spark.sql.functions.{col, collect_list, explode, udf}
			val explodedData = indexedIn.select(
				col(tempCol),
				explode(col(inputDataCol)).alias(codebookDataCol))
			val projCodebook = codebook.select(
				col(codebookIdCol).alias(tempCol),
				col(codebookDataCol))
			val vectorizeUdf = udf((matches: Array[Long]) => Histogram.compute(matches, codebookSize))
			NearestNeighbor.join(projCodebook, explodedData, Seq(tempCol), codebookDataCol, codebookIdCol)
				.select(col(tempCol), col(codebookIdCol).getField(tempCol).alias(codebookIdCol))
				.groupBy(col(tempCol))
				.agg(collect_list(codebookIdCol).alias(outputDataCol))
				.withColumn(outputDataCol, vectorizeUdf(col(outputDataCol)))
		}
		val joint = indexedIn.join(indexedOut, tempCol).drop(tempCol)
		indexedIn.unpersist
		joint
	}

}