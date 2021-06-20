package com.github.francescozoccheddu.bdp_image_classifier.pipeline.data

import com.github.francescozoccheddu.bdp_image_classifier.configuration.{DataConfig, LoadMode, Loader, SampleConfig}
import com.github.francescozoccheddu.bdp_image_classifier.pipeline.LoaderStage
import com.github.francescozoccheddu.bdp_image_classifier.pipeline.data.DataStage._
import com.github.francescozoccheddu.bdp_image_classifier.pipeline.utils.Columns.{colName, resColName}
import com.github.francescozoccheddu.bdp_image_classifier.pipeline.utils.DataTypeImplicits.DataTypeExtension
import com.github.francescozoccheddu.bdp_image_classifier.pipeline.utils.SampleUtils
import com.github.francescozoccheddu.bdp_image_classifier.utils.FileUtils
import org.apache.spark.sql.functions.{abs, col}
import org.apache.spark.sql.types.{BinaryType, BooleanType, IntegerType}
import org.apache.spark.sql.{DataFrame, SparkSession}

private[pipeline] final class DataStage(loader: Option[Loader[DataConfig]], val labelCol: String, val isTestCol: String, val imageCol: String)(implicit spark: SparkSession, fileUtils: FileUtils)
  extends LoaderStage[DataFrame, DataConfig]("Data", loader)(fileUtils) {

	private val merger: Merger = new Merger()(fileUtils)

	def this(loader: Option[Loader[DataConfig]])(implicit spark: SparkSession, fileUtils: FileUtils) = this(loader, defaultLabelCol, defaultIsTestCol, defaultImageCol)(spark, fileUtils)

	override protected def save(result: DataFrame): Unit = {}

	override protected def validate(result: DataFrame): Unit = {
		val schema = result.schema
		schema.requireField(imageCol, BinaryType)
		schema.requireField(isTestCol, BooleanType)
		schema.requireField(labelCol, IntegerType)
		require(schema.fields.length == 3)
	}

	override protected def make(): DataFrame = {
		val save = loadMode match {
			case LoadMode.MakeAndSave | LoadMode.LoadOrMakeAndSave => Some(file)
			case _ => None
		}
		val outputFile = save.getOrElse(config.tempFile)
		val sources = Array.ofDim[Option[Seq[(Int, String)]]](3)
		sources(0) = config.dataSet.map(encodeFiles(_, config.testSample))
		sources(1) = config.trainingSet.map(encodeFiles(_, false))
		sources(2) = config.testSet.map(encodeFiles(_, true))
		if (save.isEmpty)
			fileUtils.addTempFile(outputFile)
		fileUtils.makeDirs(FileUtils.parent(outputFile))
		merger.mergeFiles(sources.flatten.flatten.toSeq, outputFile)
		load(outputFile)
	}

	private def encodeFiles(classFiles: Seq[Seq[String]], isTest: Boolean): Seq[(Int, String)] =
		explodeFiles(classFiles)
		  .map(p => encode(p, isTest))

	private def explodeFiles(classFiles: Seq[Seq[String]]): Seq[(Int, String)] =
		resolveFiles(classFiles)
		  .flatMap(zip => zip._1.map((zip._2, _)))

	private def resolveFiles(classFiles: Seq[Seq[String]]): Seq[(Seq[String], Int)] =
		classFiles
		  .map(_.flatMap(fileUtils.glob))
		  .zipWithIndex

	private def encodeFiles(classFiles: Seq[Seq[String]], testSample: SampleConfig): Seq[(Int, String)] =
		SampleUtils
		  .split(classFiles.map(_.flatMap(fileUtils.glob)), testSample)
		  .zipWithIndex
		  .flatMap { case ((test, train), label) => encode(test, label, true) ++ encode(train, label, false) }

	protected override def load(): DataFrame = load(file)

	private def load(file: String): DataFrame = {
		merger.load(file, keyCol, dataCol)
		  .select(
			  col(dataCol).alias(imageCol),
			  (col(keyCol) < 0).alias(isTestCol),
			  (abs(col(keyCol)) - 1).alias(labelCol))
	}

}

private[pipeline] object DataStage {

	val defaultLabelCol: String = colName("label")
	val defaultIsTestCol: String = colName("isTest")
	val defaultImageCol: String = colName("image")

	private val keyCol: String = resColName("key")
	private val dataCol: String = resColName("data")

	private def encode(paths: Iterable[String], label: Int, isTest: Boolean): Iterable[(Int, String)] =
		paths.map(encode(_, label, isTest))

	private def encode(path: String, label: Int, isTest: Boolean): (Int, String) =
		if (isTest)
			(-label - 1, path)
		else
			(label + 1, path)

	private def encode(file: (Int, String), isTest: Boolean): (Int, String) =
		encode(file._2, file._1, isTest)

}
