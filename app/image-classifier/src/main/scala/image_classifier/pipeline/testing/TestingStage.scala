package image_classifier.pipeline.testing

import image_classifier.configuration.TestingConfig
import image_classifier.pipeline.Stage
import image_classifier.pipeline.testing.TestingStage.logger
import image_classifier.pipeline.training.TrainingStage
import image_classifier.utils.FileUtils
import org.apache.log4j.Logger
import org.apache.spark.mllib.evaluation.MulticlassMetrics
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.types.DoubleType

private[pipeline] final class TestingStage(config: Option[TestingConfig], val trainingStage: TrainingStage)(implicit spark: SparkSession, fileUtils: FileUtils)
  extends Stage[Unit, TestingConfig]("Testing", config) {

	override protected def run(specs: TestingConfig): Unit = {
		val model = trainingStage.result
		val featurizationStage = trainingStage.featurizationStage
		val dataStage = featurizationStage.dataStage
		val test = featurizationStage
		  .result
		  .filter(col(dataStage.isTestCol))
		  .repartition(spark.sparkContext.defaultParallelism)
		val data = model
		  .transform(test)
		  .select(col(trainingStage.predictionCol), col(dataStage.labelCol).cast(DoubleType))
		  .rdd
		  .map(r => (r.getDouble(0), r.getDouble(1)))
		val metrics = new MulticlassMetrics(data)
		val summary = TestingStage.print(metrics, specs.labels)
		if (specs.save.isDefined) {
			logger.info(s"Writing metrics to '${specs.save.get}'")
			fileUtils.writeString(specs.save.get, summary)
		}
		if (specs.print) {
			logger.info(s"Writing metrics to stdout")
			println()
			println(summary)
		}
	}

}

private[testing] object TestingStage {

	private val logger: Logger = Logger.getLogger(getClass)

	private def print(metrics: MulticlassMetrics, labels: Option[Seq[String]]): String = {
		val printer = new Printer
		printer.addSection("Summary")
		printer.addPercent("Accuracy", metrics.accuracy)
		printer.addPercent("F-Measure", metrics.weightedFMeasure)
		printer.addPercent("Precision", metrics.weightedPrecision)
		printer.addPercent("True positives (Recall)", metrics.weightedTruePositiveRate)
		printer.addPercent("False positives", metrics.weightedFalsePositiveRate)
		printer.add("Confusion matrix", metrics.confusionMatrix.toString(1000, 1000))
		for (l <- metrics.labels) {
			val i = l.toInt
			printer.addSection(s"Class '${labels.map(_ (i)).getOrElse(i.toString)}'")
			printer.addPercent("F-Measure", metrics.fMeasure(l))
			printer.addPercent("Precision", metrics.precision(l))
			printer.addPercent("True positives (Recall)", metrics.truePositiveRate(l))
			printer.addPercent("False positives", metrics.falsePositiveRate(l))
		}
		printer.get
	}

	private final class Printer {

		private val minSpace: Int = 32
		private val builder: StringBuilder = new StringBuilder(4096)

		def addPercent(key: String, value: Double): Unit =
			add(key, truncate(value * 100) + "%")

		private def truncate(value: Double): String = "%.3f".format(value)

		def add(key: String, value: String): Unit = {
			val header = s"$key:  "
			builder.append(header)
			addAligned(value, header.length)
			builder.append('\n')
		}

		private def addAligned(value: String, column: Int): Unit = {
			val space = minSpace max column
			val body = (value + " ")
			  .lines
			  .map(" " * space + _.trim)
			  .mkString("\n")
			  .substring(column)
			builder.append(body)
		}

		def addSection(key: String): Unit = {
			if (builder.nonEmpty)
				builder.append('\n')
			builder.append(s"--- $key ---\n")
		}

		def get: String = builder.toString

	}

}
