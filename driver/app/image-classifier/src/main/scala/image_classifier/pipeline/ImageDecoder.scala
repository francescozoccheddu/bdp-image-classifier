package image_classifier.pipeline

import org.apache.spark.ml.UnaryTransformer
import org.apache.spark.ml.util.{DefaultParamsReadable, DefaultParamsWritable, Identifiable}
import org.apache.spark.sql.Row
import org.apache.spark.sql.types.{BinaryType, DataType, IntegerType, StructField, StructType}
import com.fasterxml.jackson.module.scala.deser.overrides
import image_classifier.utils.Image

final class ImageDecoder(override val uid: String)
	extends UnaryTransformer[Array[Byte], Image, ImageDecoder]
		with DefaultParamsWritable {

	override protected def createTransformFunc: Array[Byte] => Image = {
		(data: Array[Byte]) => {
			val mat = Image.decode(data)
			Image.fromMat(mat)
		}
	}

	override protected def outputDataType: DataType = ImageDecoder.outputDataType

	override protected def validateInputType(inputType: DataType) = require(inputType == BinaryType)

	def this() = this(Identifiable.randomUID(ImageDecoder.getClass.getName))

}

object ImageDecoder extends DefaultParamsReadable[ImageDecoder] {
	import org.apache.spark.sql.catalyst.ScalaReflection

	override def load(path: String): ImageDecoder = super.load(path)

	val outputDataType = ScalaReflection.schemaFor[Image].dataType

}
