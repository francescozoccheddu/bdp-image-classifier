package image_classifier.features

import org.bytedeco.javacpp.opencv_core.Mat

private[features] object ImageReader {

	def read(width: Int, height: Int, mode: Int, data: Array[Byte]): Mat = {
		require(width > 0 && width < 8192)
		require(height > 0 && height < 8192)
		val mat = new Mat(width, height, mode)
		mat.data.put(data, 0, data.length)
		mat
	}

	def limitSize(maxSize: Int, image: Mat): Mat = {
		val size = math.max(image.rows, image.cols)
		if (size > maxSize) {
			import org.bytedeco.javacpp.opencv_imgproc.{resize, INTER_AREA}
			import org.bytedeco.javacpp.opencv_core.Size
			val scale = maxSize / size
			val dest = new Mat
			resize(image, dest, new Size, scale, scale, INTER_AREA)
			dest
		} else
			image
	}

}
