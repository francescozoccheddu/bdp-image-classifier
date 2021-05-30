package image_classifier.pipeline.featurization

import java.nio.DoubleBuffer
import image_classifier.configuration.{DescriptorConfig, ImageFeatureAlgorithm}
import org.apache.spark.ml.linalg.{Vectors, Vector => MLVector}
import org.bytedeco.javacpp.opencv_core.{CV_64F, CV_64FC1, KeyPointVector, Mat}
import org.bytedeco.javacpp.opencv_features2d.{Feature2D, ORB}
import org.bytedeco.javacpp.opencv_xfeatures2d.{SIFT, SURF}

private[featurization] final case class Descriptor(config: DescriptorConfig) {

	lazy val detector: Feature2D = {
		config.algorithm match {
			case ImageFeatureAlgorithm.Sift => SIFT.create(config.featureCount, config.octaveLayerCount, config.contrastThreshold, config.edgeThreshold, config.sigma)
			case ImageFeatureAlgorithm.Surf => SURF.create(config.hessianThreshold, config.octaveCount, config.octaveLayerCount, false, false)
			case ImageFeatureAlgorithm.Orb => ORB.create(config.featureCount, 1.2f, 8, config.edgeThreshold.toInt, 0, 2, ORB.HARRIS_SCORE, 31, 20)
		}
	}

	def apply(data: Array[Byte]): Array[MLVector] = {
		val img = Image.decode(data)
		val resizedImg = Image.limitSize(img, config.maxSize)
		describe(resizedImg)
	}

	private def describe(image: Mat): Array[MLVector] = {
		val size = detector.descriptorSize
		val kpv = new KeyPointVector
		val rawDesMat = new Mat
		detector.detectAndCompute(image, new Mat, kpv, rawDesMat)
		val kpCount = kpv.size.toInt min config.featureCount
		val buffer = if (kpCount != 0) {
			val desMat = {
				require(rawDesMat.channels == 1)
				if (rawDesMat.depth != CV_64F) {
					val mat = new Mat(kpCount, size, CV_64FC1)
					rawDesMat.convertTo(mat, CV_64F)
					mat
				}
				else rawDesMat
			}
			desMat.createBuffer[DoubleBuffer]()
		} else null
		val desArr = Array.ofDim[MLVector](kpCount)
		for (d <- 0 until kpCount) {
			val row = Array.ofDim[Double](size)
			buffer.get(row, 0, size)
			desArr(d) = Vectors.dense(row)
		}
		desArr
	}

}

