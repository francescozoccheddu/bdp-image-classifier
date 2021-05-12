package image_classifier.utils

import org.bytedeco.javacpp.opencv_core.Mat

final case class Image (width : Int, height : Int, mode : Int, data : Array[Byte]) {

	require(width > 4)
	require(height > 4)

	def toMat : Mat = Image.toMat(width, height, mode, data)

}

object Image {
	
	def decode(data : Array[Byte]) : Mat = {
		import org.bytedeco.javacpp.opencv_imgcodecs.{imdecode, IMREAD_GRAYSCALE}
		imdecode(new Mat(data, false), IMREAD_GRAYSCALE)
	}

	def fromMat(mat : Mat) : Image = {
		Image(mat.cols, mat.rows, mat.`type`, mat.data().asBuffer().array())
	}

	def toMat(width: Int, height: Int, mode: Int, data: Array[Byte]): Mat = {
		require(width >= 4 && width <= math.pow(2, 13))
		require(height >= 4 && height <= math.pow(2, 13))
		val mat = new Mat(width, height, mode)
		mat.data.put(data, 0, data.length)
		mat
	}

	def limitSize(maxSize: Int, mat: Mat): Mat = {
		require(maxSize >= 4 && maxSize <= 8192)
		val size = math.max(mat.rows, mat.cols)
		if (size > maxSize) {
			import org.bytedeco.javacpp.opencv_imgproc.{resize, INTER_AREA}
			import org.bytedeco.javacpp.opencv_core.Size
			val scale = maxSize.toDouble / size.toDouble
			val dest = new Mat
			resize(mat, dest, new Size(), scale, scale, INTER_AREA)
			dest
		} else
			mat
	}

}
