package image_classifier.utils

import org.bytedeco.javacpp.opencv_features2d.Feature2D
import org.bytedeco.javacpp.opencv_core.Mat
import org.apache.spark.ml.linalg.{Vector => MLVector}
import image_classifier.utils.ImageFeaturizer.{defaultFeatureCount, defaultAlgorithm}
import image_classifier.configuration.ImageFeatureAlgorithm
import image_classifier.configuration.ImageFeatureAlgorithm._

final case class ImageFeaturizer (featureCount : Int = defaultFeatureCount, algorithm : ImageFeatureAlgorithm = defaultAlgorithm) {
	
	require(featureCount > 0)
	
	lazy val detector : Feature2D = {
		import org.bytedeco.javacpp.opencv_xfeatures2d.{SIFT, SURF}
		algorithm match {
			case ImageFeatureAlgorithm.Sift => SIFT.create(featureCount, 3, 0.04, 10, 1.6)
		}
	}
	
	def apply(image : Mat): Array[MLVector] = {
		import java.nio.DoubleBuffer
		import org.bytedeco.javacpp.opencv_core.{CvType, KeyPointVector, Mat, CV_64F, CV_64FC1}
		import org.bytedeco.javacpp.opencv_imgcodecs.imread
		import org.apache.spark.ml.linalg.Vectors
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

object ImageFeaturizer {
	val defaultFeatureCount : Int = 10
	val defaultAlgorithm : ImageFeatureAlgorithm = ImageFeatureAlgorithm.Sift
}
