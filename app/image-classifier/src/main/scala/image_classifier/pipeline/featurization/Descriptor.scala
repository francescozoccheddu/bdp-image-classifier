package image_classifier.pipeline.featurization

import image_classifier.configuration.ImageFeatureAlgorithm
import image_classifier.configuration.ImageFeatureAlgorithm._
import org.apache.spark.ml.linalg.{Vector => MLVector}
import org.bytedeco.javacpp.opencv_core.Mat
import org.bytedeco.javacpp.opencv_features2d.Feature2D

private[image_classifier] trait DescriptorConfig {

	def algorithm : ImageFeatureAlgorithm
	def featureCount : Int
	def octaveLayerCount : Int
	def contrastThreshold : Double
	def edgeThreshold : Double
	def sigma : Double

}

private[featurization] final case class Descriptor(config : DescriptorConfig) {

	lazy val detector: Feature2D = {
		import org.bytedeco.javacpp.opencv_xfeatures2d.SIFT
		config.algorithm match {
			case ImageFeatureAlgorithm.Sift => SIFT.create(config.featureCount, config.octaveLayerCount, config.contrastThreshold, config.edgeThreshold, config.sigma)
		}
	}

	def apply(image: Mat): Array[MLVector] = {
		import org.apache.spark.ml.linalg.Vectors
		import org.bytedeco.javacpp.opencv_core.{CV_64F, CV_64FC1, KeyPointVector, Mat}
		import java.nio.DoubleBuffer
		val size = detector.descriptorSize
		val kpv = new KeyPointVector
		val rawDesMat = new Mat
		detector.detectAndCompute(image, new Mat, kpv, rawDesMat)
		val kpCount = kpv.size.toInt
		val desMat = {
			require(rawDesMat.channels == 1)
			if (rawDesMat.depth != CV_64F) {
				val mat = new Mat(kpCount, size, CV_64FC1)
				rawDesMat.convertTo(mat, CV_64F)
				mat
			}
			else rawDesMat
		}
		val buffer = desMat.createBuffer[DoubleBuffer]()
		val desArr = Array.ofDim[MLVector](kpCount)
		for (d <- 0 until kpCount) {
			val row = Array.ofDim[Double](size)
			buffer.get(row, 0, size)
			desArr(d) = Vectors.dense(row)
		}
		desArr
	}

}

