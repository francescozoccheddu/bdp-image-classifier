package image_classifier.pipeline.featurization

import org.bytedeco.javacpp.opencv_core.Mat

private[featurization] object Image {

	def decode(data: Array[Byte]): Mat = {
		import org.bytedeco.javacpp.opencv_imgcodecs.{IMREAD_GRAYSCALE, imdecode}
		imdecode(new Mat(data, false), IMREAD_GRAYSCALE)
	}

	def limitSize(mat: Mat, maxSize: Int): Mat = {
		require(maxSize >= 4 && maxSize <= 8192)
		val size = math.max(mat.rows, mat.cols)
		if (size > maxSize) {
			import org.bytedeco.javacpp.opencv_core.Size
			import org.bytedeco.javacpp.opencv_imgproc.{INTER_AREA, resize}
			val scale = maxSize.toDouble / size.toDouble
			val dest = new Mat
			resize(mat, dest, new Size(), scale, scale, INTER_AREA)
			dest
		} else
			mat
	}

}
