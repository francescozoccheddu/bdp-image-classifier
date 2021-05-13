package image_classifier.pipeline

import image_classifier.utils.Image
import org.apache.spark.ml.UnaryTransformer
import org.apache.spark.ml.util.{DefaultParamsReadable, DefaultParamsWritable, Identifiable}
import org.apache.spark.sql.Row
import org.apache.spark.sql.types.DataType
import org.apache.spark.ml.linalg.{Vector => MLVector}

final class ImageFeaturizer(override val uid: String)
	extends UnaryTransformer[Image, Array[MLVector], ImageFeaturizer]
		with DefaultParamsWritable
		with HasFeaturesCount {

	override protected def createTransformFunc: Image => Array[MLVector] = {
		import image_classifier.utils.{ImageFeaturizer => Featurizer}
		val featurizer = Featurizer($(featuresCount))
		(image: Image) => {
			featurizer(image.toMat)
		}
	}

	override protected def outputDataType: DataType = ImageFeaturizer.outputDataType

	override protected def validateInputType(inputType: DataType) = require(inputType == ImageDecoder.outputDataType)

	def this() = this(Identifiable.randomUID(ImageFeaturizer.getClass.getName))

}

object ImageFeaturizer extends DefaultParamsReadable[ImageFeaturizer] {

	override def load(path: String): ImageFeaturizer = super.load(path)

	val outputDataType = {
		import org.apache.spark.sql.types.ArrayType
		import org.apache.spark.ml.linalg.SQLDataTypes.VectorType
		ArrayType(VectorType)
	}

}