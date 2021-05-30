package image_classifier.pipeline.data

import scala.util.Random
import image_classifier.configuration.{DataConfig, LoadMode, Loader}
import image_classifier.pipeline.Columns.{colName, resColName}
import image_classifier.pipeline.LoaderStage
import image_classifier.pipeline.data.DataStage._
import image_classifier.utils.DataTypeImplicits.DataTypeExtension
import image_classifier.utils.FileUtils
import org.apache.log4j.Logger
import org.apache.spark.sql.functions.{abs, col}
import org.apache.spark.sql.types.{BinaryType, BooleanType, IntegerType}
import org.apache.spark.sql.{DataFrame, SparkSession}

private[pipeline] final class DataStage(loader: Option[Loader[DataConfig]], val labelCol: String, val isTestCol: String, val imageCol: String)(implicit spark: SparkSession, fileUtils: FileUtils)
  extends LoaderStage[DataFrame, DataConfig]("Data", loader)(fileUtils) {

	private val merger: Merger = new Merger()(spark)

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
		sources(0) = config.dataSet.map(encodeFiles(_, config.testFraction, config.splitSeed, config.stratified))
		sources(1) = config.trainingSet.map(encodeFiles(_, false))
		sources(2) = config.testSet.map(encodeFiles(_, true))
		if (save.isEmpty)
			fileUtils.addTempFile(outputFile)
		fileUtils.makeDirs(FileUtils.parent(outputFile))
		merger.mergeFiles(sources.flatten.flatten.toSeq, outputFile)
		load(outputFile)
	}

	protected override def load(): DataFrame = load(file)

	private def load(file: String): DataFrame = {
		if (!FileUtils.isValidHDFSPath(file))
			logger.warn("Loading from a local path hampers parallelization")
		merger.load(file, keyCol, dataCol)
		  .select(
			  col(dataCol).alias(imageCol),
			  (col(keyCol) < 0).alias(isTestCol),
			  (abs(col(keyCol)) - 1).alias(labelCol))
	}

	private def resolveFiles(classFiles: Seq[Seq[String]]): Seq[(Seq[String], Int)] =
		classFiles
		  .map(_.flatMap(fileUtils.glob))
		  .zipWithIndex

	private def explodeFiles(classFiles: Seq[Seq[String]]): Seq[(Int, String)] =
		resolveFiles(classFiles)
		  .flatMap(zip => zip._1.map((zip._2, _)))

	private def encodeFiles(classFiles: Seq[Seq[String]], isTest: Boolean): Seq[(Int, String)] =
		explodeFiles(classFiles)
		  .map(p => encode(p, isTest))

	private def encodeFiles(classFiles: Seq[Seq[String]], testFraction: Double, testSeed: Int): Seq[(Int, String)] =
		encode(explodeFiles(classFiles), testFraction, testSeed)

	private def encodeFiles(classFiles: Seq[Seq[String]], testFraction: Double, testSeed: Int, stratified: Boolean): Seq[(Int, String)] =
		if (stratified)
			encodeFilesStratified(classFiles, testFraction, testSeed)
		else
			encodeFiles(classFiles, testFraction, testSeed)

	private def encodeFilesStratified(classFiles: Seq[Seq[String]], testFraction: Double, testSeed: Int): Seq[(Int, String)] =
		resolveFiles(classFiles)
		  .flatMap(zip => encode(zip._1.map((zip._2, _)), testFraction, testSeed))

}

private[pipeline] object DataStage {

	val defaultLabelCol: String = colName("label")
	val defaultIsTestCol: String = colName("isTest")
	val defaultImageCol: String = colName("image")

	private val keyCol: String = resColName("key")
	private val dataCol: String = resColName("data")
	private val logger: Logger = Logger.getLogger(getClass)

	private def encode(files: Seq[(Int, String)], testFraction: Double, testSeed: Int): Seq[(Int, String)] = {
		val count = files.length * testFraction
		new Random(testSeed)
		  .shuffle(files)
		  .zipWithIndex
		  .map(p => encode(p._1, p._2 < count))
	}

	private def encode(file: (Int, String), isTest: Boolean): (Int, String) = {
		val (label, path) = file
		if (isTest)
			(-label - 1, path)
		else
			(label + 1, path)
	}

}
