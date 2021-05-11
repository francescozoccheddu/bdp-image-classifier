package image_classifier.pipeline

import org.apache.spark.ml.UnaryTransformer
import org.apache.spark.ml.util.{ DefaultParamsReadable, DefaultParamsWritable, Identifiable }
import org.apache.spark.sql.Row
import org.apache.spark.ml.linalg.SQLDataTypes.VectorType
import org.apache.spark.sql.types.{DataType, ArrayType}
import org.apache.spark.ml.linalg.{Vector => MLVector}

class ImageFeaturizer(override val uid: String) 
	extends UnaryTransformer[Row, Array[MLVector], ImageFeaturizer] 
	with DefaultParamsWritable 
	with HasFeaturesCount with HasImageWidthCol with HasImageHeightCol with HasImageTypeCol with HasImageDataCol {

	override protected def createTransformFunc : Row => Array[MLVector] = {
		import image_classifier.utils.{Image, ImageFeaturizer => Featurizer}
		val featurizer = Featurizer($(featuresCount))
		(data : Row) => {
			val image = Image(data.getAs[Int]($(imageWidthCol)), data.getAs[Int]($(imageHeightCol)), data.getAs[Int]($(imageTypeCol)), data.getAs[Array[Byte]]($(imageDataCol)))
			featurizer(image.toMat)
		}
	}

	override protected def outputDataType : DataType = ArrayType(VectorType)

	def this() = this(Identifiable.randomUID(ImageFeaturizer.getClass.getName))

}

object ImageFeaturizer extends DefaultParamsReadable[ImageFeaturizer] {

	override def load(path: String) : ImageFeaturizer = super.load(path)

}