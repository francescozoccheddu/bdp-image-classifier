package image_classifier.pipeline

import org.apache.spark.ml.UnaryTransformer
import org.apache.spark.ml.util.{ DefaultParamsReadable, DefaultParamsWritable, Identifiable }
import org.apache.spark.sql.Row
import org.apache.spark.sql.types.{DataType, StructType, IntegerType, BinaryType, StructField}

class ImageDecoder(override val uid: String) 
extends UnaryTransformer[Array[Byte], Row, ImageDecoder] 
with DefaultParamsWritable 
	with HasImageWidthCol with HasImageHeightCol with HasImageTypeCol with HasImageDataCol {

	override protected def createTransformFunc : Array[Byte] => Row = {
		import image_classifier.utils.Image
		(data : Array[Byte]) => {
			val mat = Image.decode(data)
			val image = Image.fromMat(mat)
			Row(image.width, image.height, image.mode, image.data)
		}
	}

	override protected def outputDataType : DataType = 
		StructType(
			StructField($(imageWidthCol), IntegerType, false) ::
			StructField($(imageHeightCol), IntegerType, false) ::
			StructField($(imageTypeCol), IntegerType, false) ::
			StructField($(imageDataCol), BinaryType, false) :: 
			Nil)

	def this() = this(Identifiable.randomUID(ImageDecoder.getClass.getName))

}

object ImageDecoder extends DefaultParamsReadable[ImageDecoder] {

	override def load(path: String) : ImageDecoder = super.load(path)

}
