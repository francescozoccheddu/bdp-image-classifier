package image_classifier.pipeline.featurization

import org.apache.spark.sql.DataFrame

private[featurization] object BOWV {
	import image_classifier.pipeline.utils.Columns.{colName, resColName}
	import org.apache.spark.sql.functions.col

	private val codebookDataCol = resColName("data")
	private val codebookIdCol = resColName("id")
	private val idCol = resColName("id")
	private val neighborCol = resColName("neighbor")
	val defaultOutputCol: String = colName("data")

	private def projectForCodebookJoin(data: DataFrame, inputCol: String) = {
		import org.apache.spark.sql.types.IntegerType
		val field = data.schema.fields.find(_.dataType == IntegerType)
		val newData = if (field.isDefined) {
			data.withColumnRenamed(field.get.name, codebookIdCol)
		}
		else {
			import org.apache.spark.sql.functions.lit
			data.withColumn(codebookIdCol, lit(0))
		}
		newData.select(col(codebookIdCol), col(inputCol).alias(codebookDataCol))
	}

	def createCodebook(data: DataFrame, size: Int, inputCol: String, assignNearest: Boolean): DataFrame = {
		{
			import image_classifier.utils.DataTypeImplicits._
			import org.apache.spark.ml.linalg.SQLDataTypes.VectorType
			data.schema.requireField(inputCol, VectorType)
		}
		import data.sparkSession.implicits._
		import org.apache.spark.ml.clustering.KMeans
		import org.apache.spark.sql.functions.col
		val model = new KMeans().setK(size).setMaxIter(200).setFeaturesCol(inputCol).fit(data)
		val centers = model
			.clusterCenters
			.toSeq
			.zipWithIndex
			.toDF(codebookDataCol, codebookIdCol)
		(if (assignNearest) {
			val dataWithId = projectForCodebookJoin(data, inputCol)
			NearestNeighbor.join(dataWithId, centers, Seq(codebookIdCol, codebookDataCol), codebookDataCol, neighborCol)
				.select(col(codebookIdCol), col(neighborCol).getField(codebookDataCol).alias(codebookDataCol))
		}
		else
			centers)
			.dropDuplicates(codebookDataCol)
	}

	def compute(data: DataFrame, codebook: DataFrame, inputCol: String, outputCol: String = defaultOutputCol): DataFrame = {
		import org.apache.spark.sql.types.IntegerType
		{
			import image_classifier.utils.DataTypeImplicits._
			import org.apache.spark.ml.linalg.SQLDataTypes.VectorType
			import org.apache.spark.sql.types.{ArrayType, LongType}
			val dataSchema = data.schema
			dataSchema.requireField(inputCol, ArrayType(VectorType))
			val codebookSchema = codebook.schema
			codebookSchema.requireField(codebookDataCol, VectorType)
			codebookSchema.requireField(codebookIdCol, IntegerType)
		}
		val indexedIn = {
			import org.apache.spark.sql.functions.monotonically_increasing_id
			data.withColumn(idCol, monotonically_increasing_id).cache
		}
		val indexedOut = {
			import org.apache.spark.sql.functions.{col, collect_list, explode, udf}
			import org.apache.spark.sql.types.LongType
			val explodedData = indexedIn.select(
				col(idCol),
				explode(col(inputCol)).alias(codebookDataCol))
			val projCodebook = codebook.select(
				col(codebookIdCol).alias(idCol).cast(LongType),
				col(codebookDataCol)).cache
			val codebookSize = projCodebook.count.toInt
			val vectorizeUdf = udf((matches: Array[Long]) => Histogram.compute(matches, codebookSize))
			val result = NearestNeighbor.join(projCodebook, explodedData, Seq(idCol), codebookDataCol, neighborCol)
				.select(col(idCol), col(neighborCol).getField(idCol).alias(outputCol))
				.groupBy(col(idCol))
				.agg(collect_list(outputCol).alias(outputCol))
				.withColumn(outputCol, vectorizeUdf(col(outputCol)))
			projCodebook.unpersist
			result
		}
		val joint = indexedIn.drop(outputCol).join(indexedOut, idCol).drop(idCol)
		indexedIn.unpersist
		joint
	}

}