package image_classifier.pipeline.featurization

import org.bytedeco.javacpp.opencv_core.{Mat, Size}
import org.bytedeco.javacpp.opencv_imgcodecs.{IMREAD_GRAYSCALE, imdecode}
import org.bytedeco.javacpp.opencv_imgproc.{INTER_AREA, resize}

private[featurization] object Image {

	def decode(data: Array[Byte]): Mat = {
		val rawMat = new Mat(data, false)
		val mat = imdecode(rawMat, IMREAD_GRAYSCALE)
		rawMat.deallocate()
		mat
	}

	def limitSize(mat: Mat, maxSize: Int): Mat = {
		require(maxSize >= 4 && maxSize <= 8192)
		val size = math.max(mat.rows, mat.cols)
		if (size > maxSize) {
			val scale = maxSize.toDouble / size.toDouble
			val dest = new Mat
			val nativeSize = new Size()
			resize(mat, dest, new Size(), scale, scale, INTER_AREA)
			nativeSize.deallocate()
			mat.deallocate()
			dest
		} else
			mat
	}

}
