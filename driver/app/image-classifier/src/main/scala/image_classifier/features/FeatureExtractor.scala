package image_classifier.features

private[features] object FeatureExtractor {

	

	def describe(imageFile: String): IndexedSeq[linalg.Vector] = {
		import DescriptorFactory.siftSize
		import org.bytedeco.javacpp.opencv_core.{CvType, KeyPointVector, Mat}
		import org.bytedeco.javacpp.opencv_imgcodecs.imread
		import org.apache.spark.mllib.linalg.Vectors
		val img = imread(imageFile)
		val kp = new KeyPointVector
		sift.detect(img, kp)
		val kpCount = kp.size.toInt
		val desMat = new Mat(kpCount, siftSize)
		sift.compute(img, kp, desMat)
		for (d <- 0 until kpCount) yield {
			val desRow = Array.ofDim[Double](siftSize)
			val row = desMat.row(d).data
			for (i <- 0 until siftSize) desRow(i) = row.getDouble(i)
			Vectors.dense(desRow)
		}
	}

}