package image_classifier.pipeline.featurization

import java.awt.image.{BufferedImage, DataBufferByte}
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.opencv_core.{CV_8UC1, Mat, Size}
import org.bytedeco.javacpp.opencv_imgcodecs.{IMREAD_GRAYSCALE, imdecode}
import org.bytedeco.javacpp.opencv_imgproc.{INTER_AREA, resize}

private[featurization] object Image {

	def decode(data: Array[Byte], useImageIO: Boolean): Mat = {
		val mat = if (useImageIO)
			decodeWithImageIO(data)
		else {
			val opencvMat = decodeWithOpenCV(data)
			if (opencvMat.rows > 0 && opencvMat.cols > 0) opencvMat
			else {
				opencvMat.deallocate()
				decodeWithImageIO(data)
			}
		}
		mat
	}

	private def decodeWithOpenCV(data: Array[Byte]): Mat = {
		val rawMat = new Mat(data, false)
		val mat = imdecode(rawMat, IMREAD_GRAYSCALE)
		rawMat.deallocate()
		mat
	}

	private def decodeWithImageIO(data: Array[Byte]): Mat = {
		val array = new ByteArrayInputStream(data)
		val image = {
			val original = ImageIO.read(array)
			if (original.getType == BufferedImage.TYPE_BYTE_GRAY) original
			else {
				val greyscale = new BufferedImage(original.getWidth, original.getHeight, BufferedImage.TYPE_BYTE_GRAY)
				val graphics = greyscale.getGraphics
				graphics.drawImage(original, 0, 0, null)
				graphics.dispose()
				greyscale
			}
		}
		val pointer = new BytePointer(image.getRaster.getDataBuffer.asInstanceOf[DataBufferByte].getData: _*)
		new Mat(image.getHeight, image.getWidth, CV_8UC1, pointer)
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
