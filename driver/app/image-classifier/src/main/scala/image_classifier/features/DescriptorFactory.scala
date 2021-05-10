package image_classifier.features

import DescriptorFactory.{defaultFeaturesCount, defaultMaxImageSize}
import org.apache.spark.ml.linalg.{Vector => MLVector}

case class DescriptorFactory (featuresCount : Int = defaultFeaturesCount, maxImageSize : Int = defaultMaxImageSize) {

	private def this() = {
		this(defaultFeaturesCount, defaultMaxImageSize)
	}
	
	require(featuresCount >= 1 && featuresCount < 1000)
	require(maxImageSize >= 4 && maxImageSize <= 8192)

	lazy val detector = FeatureExtractor.createDetector(featuresCount)

	def describe(width : Int, height : Int, mode : Int, data : Array[Byte]): Seq[MLVector] = {
		val image = ImageReader.read(width, height, mode, data)
		val resizedImage = ImageReader.limitSize(maxImageSize, image)
		FeatureExtractor.describe(resizedImage, detector)
	}

}

object DescriptorFactory {

	val defaultFeaturesCount : Int = 10
	val defaultMaxImageSize : Int = 512

}