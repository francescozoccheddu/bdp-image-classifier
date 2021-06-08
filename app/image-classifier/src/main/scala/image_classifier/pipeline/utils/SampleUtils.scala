package image_classifier.pipeline.utils

import scala.util.Random
import image_classifier.configuration.SampleConfig
import image_classifier.pipeline.utils.DataTypeImplicits.DataTypeExtension
import org.apache.log4j.Logger
import org.apache.spark.sql.types.IntegerType
import org.apache.spark.sql.{DataFrame, Row}

private[pipeline] object SampleUtils {

	private val logger: Logger = Logger.getLogger(getClass)

	def sample(data: DataFrame, labelCol: String, config: SampleConfig): DataFrame = {
		data.schema.requireField(labelCol, IntegerType)
		if (config.canSkip) {
			logger.info("Skipping sampling")
			return data
		}
		data.cache
		val counts = data
		  .groupBy(labelCol)
		  .count
		  .collect
		  .map((r: Row) => (r.getInt(0), r.getLong(1).toInt))
		  .toMap
		if (counts.isEmpty) {
			logger.info("Data is empty")
			return data
		}
		val fractions = computeFractions(counts, config)
		val sample = data.stat.sampleBy(labelCol, fractions, config.actualSeed)
		data.unpersist
		sample
	}

	private def computeFractions(counts: Map[Int, Int], config: SampleConfig): Map[Int, Double] = {
		val scaledCounts = computeApproximateCounts(counts, config)
		counts.transform((l, c) => (scaledCounts(l) / c) min 1.0)
	}

	def split[T](data: Seq[Seq[T]], config: SampleConfig): Seq[(Seq[T], Seq[T])] = {
		val mapData = data.view.zipWithIndex.map(_.swap).toMap
		split(mapData, config).values.toSeq
	}

	def split[T](data: Map[Int, Seq[T]], config: SampleConfig): Map[Int, (Seq[T], Seq[T])] = {
		val counts = data.mapValues(_.length)
		val leftCounts = computeCounts(counts, config)
		val random = new Random(config.actualSeed)
		data.transform((l, v) => random.shuffle(v).splitAt(leftCounts(l)))
	}

	private def computeCounts(counts: Map[Int, Int], config: SampleConfig): Map[Int, Int] =
		computeApproximateCounts(counts, config).mapValues(math.round(_).toInt)

	private def computeApproximateCounts(counts: Map[Int, Int], config: SampleConfig): Map[Int, Double] = {
		require(counts.values.forall(_ >= 0))
		val total = counts.values.sum
		if (total == 0) {
			logger.info("Data is empty")
			return counts.mapValues(_.toDouble)
		}
		val scaledCounts = {
			val trimmedCounts = {
				val limitedCounts = counts.mapValues(c => config.maxCountPerClass.toDouble min c * config.maxFractionPerClass)
				val minCount = limitedCounts.values.min
				limitedCounts.mapValues(_ min minCount * config.maxMaxMinFractionPerClass)
			}
			val trimmedTotal = trimmedCounts.values.sum
			val scale = ((config.maxCount.toDouble min config.maxFraction * total) / trimmedTotal) min 1.0
			trimmedCounts.mapValues(_ * scale)
		}
		{
			val scaledTotal = scaledCounts.values.sum
			val approxPercent = scaledTotal / total * 100
			val approxCount = math.round(scaledTotal)
			val minCount = math.round(scaledCounts.values.min)
			val maxCount = math.round(scaledCounts.values.max)
			logger.info(s"Filtering ${"%.2f".format(approxPercent)}%, $total to ~$approxCount, min ~$minCount, max ~$maxCount samples with seed ${config.actualSeed}")
		}
		scaledCounts
	}

}
