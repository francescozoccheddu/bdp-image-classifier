package image_classifier.pipeline.featurization

import org.apache.spark.sql.DataFrame

private[featurization] object BOWV {
	import image_classifier.pipeline.Columns.{colName, resColName}

	private val codebookDataCol = resColName("data")
	private val codebookIdCol = resColName("id")
	private val idCol = resColName("id")
	val defaultOutputCol: String = colName("data")

	def createCodebook(data: DataFrame, size: Int, inputCol: String, assignNearest: Boolean): DataFrame = {
		{
			import image_classifier.utils.DataTypeImplicits._
			import org.apache.spark.ml.linalg.SQLDataTypes.VectorType
			data.schema.requireField(inputCol, VectorType)
		}
		import data.sparkSession.implicits._
		import org.apache.spark.ml.clustering.KMeans
		val model = new KMeans().setK(size).setMaxIter(10).setFeaturesCol(inputCol).fit(data)
		val centers = model
			.clusterCenters
			.toSeq
			.zipWithIndex
			.toDF(codebookDataCol, codebookIdCol)
		val assignedCenters = if (assignNearest)
			new NearestNeighbor(codebookDataCol, inputCol, codebookDataCol).joinFeatures(centers, data)
		else
			centers
		assignedCenters.dropDuplicates(codebookDataCol)
	}

	def compute(data: DataFrame, codebook: DataFrame, codebookSize: Int, inputCol: String, outputCol: String = defaultOutputCol): DataFrame = {
		{
			import image_classifier.utils.DataTypeImplicits._
			import org.apache.spark.ml.linalg.SQLDataTypes.VectorType
			import org.apache.spark.sql.types.{IntegerType, ArrayType}
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
			val explodedData = indexedIn.select(
				col(idCol),
				explode(col(inputCol)).alias(inputCol))
			val vectorizeUdf = udf((matches: Array[Long]) => Histogram.compute(matches, codebookSize))
			new NearestNeighbor(inputCol, codebookDataCol, outputCol)
				.joinColumn[Int](explodedData, codebook, codebookIdCol)
				.select(col(idCol), col(outputCol))
				.groupBy(col(idCol))
				.agg(collect_list(outputCol).alias(outputCol))
				.withColumn(outputCol, vectorizeUdf(col(outputCol)))
		}
		val joint = indexedIn.drop(outputCol).join(indexedOut, idCol).drop(idCol)
		joint
	}

}