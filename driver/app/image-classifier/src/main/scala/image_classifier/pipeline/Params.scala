package image_classifier.pipeline

import org.apache.spark.ml.param.{Param, Params, IntParam}

private[pipeline] trait HasFeaturesCount extends Params {

	final val featuresCount: IntParam = new IntParam(this, "featuresCount", "features count", _ > 0)

	setDefault(featuresCount, 10)

	final def getFeaturesCount: Int = $(featuresCount)

	final def setFeaturesCount(value: Int): this.type = set(featuresCount, value)

}

private[pipeline] trait HasFeaturesCol extends Params {

	final val featuresCol: Param[String] = new Param[String](this, "featuresCol", "features column name")

	setDefault(featuresCol, "features")

	final def getFeaturesCol: String = $(featuresCol)

	final def setFeaturesCol(value: String): this.type = set(featuresCol, value)

}