package image_classifier.features

import org.bytedeco.javacpp.opencv_features2d.Feature2D
import org.bytedeco.javacpp.opencv_core.Mat
import org.apache.spark.ml.linalg.{Vector => MLVector}
import ExtractionAlgorithm._

private[features] object FeatureExtractor {
	
	def createDetector(algorithm : ExtractionAlgorithm, featuresCount : Int) : Feature2D = {
		require(featuresCount >= 1 && featuresCount < 1000)
		import org.bytedeco.javacpp.opencv_xfeatures2d.{SIFT, SURF}
		import org.bytedeco.javacpp.opencv_features2d.{ORB, FastFeatureDetector, MSER, GFTTDetector}
		algorithm match {
			case ExtractionAlgorithm.SIFT => SIFT.create(featuresCount, 3, 0.04, 10, 1.6)
			case ExtractionAlgorithm.MSER => MSER.create()
			case ExtractionAlgorithm.FAST => FastFeatureDetector.create()
			case ExtractionAlgorithm.ORB => ORB.create()
			case ExtractionAlgorithm.GFTT => GFTTDetector.create()
			case ExtractionAlgorithm.SURF => SURF.create()
		}
	}	

	def describe(image : Mat, detector : Feature2D): Seq[MLVector] = {
		import org.bytedeco.javacpp.opencv_core.{CvType, KeyPointVector, Mat}
		import org.bytedeco.javacpp.opencv_imgcodecs.imread
		import org.apache.spark.ml.linalg.Vectors
		val size = detector.descriptorSize
		val kp = new KeyPointVector
		detector.detect(image, kp)
		val kpCount = kp.size.toInt
		val desMat = new Mat(kpCount, size, detector.descriptorType)
		detector.compute(image, kp, desMat)
		val des = Array.ofDim[MLVector](kpCount)
		for (d <- 0 until kpCount) yield {
			val desRow = Array.ofDim[Double](128)
			val row = desMat.row(d).data
			for (i <- 0 until 128) desRow(i) = row.getDouble(i)
			des(d) = Vectors.dense(desRow)
		}
		des.toSeq
	}

}