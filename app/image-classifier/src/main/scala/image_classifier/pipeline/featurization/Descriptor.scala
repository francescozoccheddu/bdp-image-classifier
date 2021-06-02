package image_classifier.pipeline.featurization

import java.nio.DoubleBuffer
import image_classifier.configuration.{DescriptorConfig, ImageFeatureAlgorithm}
import org.apache.spark.ml.linalg.{Vector, Vectors}
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

	def apply(data: Array[Byte]): Seq[Vector] = {
		val img = Image.decode(data)
		val resizedImg = Image.limitSize(img, config.maxSize)
		describe(resizedImg)
	}

	private def describe(image: Mat): Seq[Vector] = {
		val size = detector.descriptorSize
		val kpv = new KeyPointVector
		val rawDesMat = new Mat
		val maskMat = new Mat
		detector.detectAndCompute(image, maskMat, kpv, rawDesMat)
		maskMat.deallocate()
		val kpCount = kpv.size.toInt min config.featureCount
		kpv.deallocate()
		val desMat = if (kpCount != 0) {
			require(rawDesMat.channels == 1)
			if (rawDesMat.depth != CV_64F) {
				val mat = new Mat(kpCount, size, CV_64FC1)
				rawDesMat.convertTo(mat, CV_64F)
				rawDesMat.deallocate()
				mat
			}
			else rawDesMat
		} else rawDesMat
		val buffer = if (kpCount != 0) desMat.createBuffer[DoubleBuffer]() else null
		val desArr = Array.ofDim[Vector](kpCount)
		for (d <- 0 until kpCount) {
			val row = Array.ofDim[Double](size)
			buffer.get(row, 0, size)
			desArr(d) = Vectors.dense(row)
		}
		desMat.deallocate()
		desArr
	}

	override def finalize(): Unit = detector.deallocate()

}

