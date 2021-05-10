package image_classifier.features

import org.bytedeco.javacpp.opencv_features2d.Feature2D
import org.bytedeco.javacpp.opencv_core.{Mat, CV_MAT_DEPTH, CV_64F, CV_64FC1}
import org.apache.spark.ml.linalg.{Vector => MLVector}
import java.nio.FloatBuffer
import java.nio.DoubleBuffer

private[features] object FeatureExtractor {
	
	def createDetector(featuresCount : Int) : Feature2D = {
		require(featuresCount >= 1 && featuresCount < 1000)
		import org.bytedeco.javacpp.opencv_xfeatures2d.SIFT
		SIFT.create(featuresCount, 3, 0.04, 10, 1.6)
	}	

	def describe(image : Mat, detector : Feature2D): Seq[MLVector] = {
		import org.bytedeco.javacpp.opencv_core.{CvType, KeyPointVector, Mat}
		import org.bytedeco.javacpp.opencv_imgcodecs.imread
		import org.apache.spark.ml.linalg.Vectors
		val size = detector.descriptorSize
		val kpv = new KeyPointVector
		val rawDesMat = new Mat
		val mask = new Mat
		detector.detectAndCompute(image, mask, kpv, rawDesMat)
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
		desArr.toSeq
	}

}