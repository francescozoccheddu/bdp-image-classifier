package image_classifier.pipeline.utils

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
		val total = counts.values.sum
		if (total == 0) {
			logger.info("Data is empty")
			return data
		}
		val trimmedCounts = {
			val minCount = counts.values.min
			counts.mapValues(c => config.maxCountPerClass.toDouble min c * config.maxFractionPerClass min minCount * config.maxMaxMinFractionPerClass)
		}
		val scaledCounts = {
			val trimmedTotal = trimmedCounts.values.sum
			val scale = ((config.maxCount.toDouble min config.maxFraction * total) / trimmedTotal) min 1.0
			trimmedCounts.mapValues(_ * scale)
		}
		{
			val scaledTotal = scaledCounts.values.sum
			val approxPercent = scaledTotal / total * 100
			val approxCount = math.round(scaledTotal)
			logger.info(s"Sampling ${"%.2f".format(approxPercent)}% of data, $total to ~$approxCount samples")
		}
		val fractions = counts.transform((l, c) => scaledCounts(l) / c min 1.0)
		val sample = data.stat.sampleBy(labelCol, fractions, config.seed)
		data.unpersist
		sample
	}

}
