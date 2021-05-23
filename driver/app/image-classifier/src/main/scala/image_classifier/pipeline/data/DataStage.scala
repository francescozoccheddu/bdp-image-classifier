package image_classifier.pipeline.data

import org.apache.spark.sql.{DataFrame, SparkSession}
import image_classifier.configuration.{DataConfig, Loader}
import image_classifier.pipeline.LoaderStage
import image_classifier.pipeline.data.DataStage.{defaultImageCol, defaultIsTestCol, defaultLabelCol}

private[pipeline] final class DataStage(loader: Option[Loader[DataConfig]], workingDir: String, val labelCol: String, val isTestCol: String, val imageCol: String)(implicit spark: SparkSession) extends LoaderStage[DataFrame, DataConfig]("Data", loader) {
	import image_classifier.pipeline.data.DataStage.encode

	def this(loader: Option[Loader[DataConfig]], workingDir: String)(implicit spark: SparkSession) = this(loader, workingDir, defaultLabelCol, defaultIsTestCol, defaultImageCol)(spark)

	override protected def save(result: DataFrame, file: String): Unit = {}

	override protected def make(config: DataConfig) = {
		import image_classifier.configuration.LoadMode
		val save = specs.get.mode match {
			case LoadMode.MakeAndSave | LoadMode.LoadOrMakeAndSave => specs.get.file
			case _ => None
		}
		val file = save.getOrElse(config.tempFile)
		val sources = Array.ofDim[Option[Seq[(Int, String)]]](3)
		sources(0) = config.dataSet.map(encodeFiles(_, config.testFraction, config.splitSeed, config.stratified))
		sources(1) = config.trainingSet.map(encodeFiles(_, false))
		sources(2) = config.testSet.map(encodeFiles(_, true))
		if (save.isEmpty)
			addTempFile(file)
		Merger.mergeFiles(sources.flatten.flatten.toSeq, file)
		load(file)
	}

	protected override def load(file: String): DataFrame = {
		import image_classifier.pipeline.data.DataStage.{keyCol, dataCol}
		import org.apache.spark.sql.functions.{col, abs}
		Merger.load(file, keyCol, dataCol)
			.select(
				col(dataCol).alias(imageCol),
				(col(keyCol) < 0).alias(isTestCol),
				(abs(col(keyCol)) - 1).alias(labelCol))
	}

	private def resolveFiles(classFiles: Seq[Seq[String]]) =
		classFiles
			.map(_.flatMap(FileUtils.listFiles(workingDir, _)))
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

	private def addTempFile(file: String) = {
		import image_classifier.pipeline.data.DataStage.sparkListener
		spark.sparkContext.removeSparkListener(sparkListener)
		spark.sparkContext.addSparkListener(sparkListener)
		FileUtils.addTempFile(file)
	}

}

private[pipeline] object DataStage {
	import image_classifier.pipeline.utils.Columns.{colName, resColName}
	import org.apache.spark.scheduler.SparkListener

	val defaultLabelCol = colName("label")
	val defaultIsTestCol = colName("isTest")
	val defaultImageCol = colName("image")

	private val keyCol = resColName("key")
	private val dataCol = resColName("data")

	private def encode(file: (Int, String), isTest: Boolean): (Int, String) = {
		val (label, path) = file
		if (isTest)
			(-label - 1, path)
		else
			(label + 1, path)
	}

	private def encode(files: Seq[(Int, String)], testFraction: Double, testSeed: Int): Seq[(Int, String)] = {
		import scala.util.Random
		val count = files.length * testFraction
		new Random(testSeed)
			.shuffle(files)
			.zipWithIndex
			.map(p => encode(p._1, p._2 < count))
	}

	private val sparkListener = new SparkListener {
		import org.apache.spark.scheduler.SparkListenerApplicationEnd

		override def onApplicationEnd(applicationEnd: SparkListenerApplicationEnd): Unit =
			FileUtils.clearTempFiles()

	}

}
