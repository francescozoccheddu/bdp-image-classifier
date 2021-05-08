package image_classifier.features

import DescriptorFactory.{defaultAlgorithm, defaultFeaturesCount, defaultMaxImageSize}
import org.apache.spark.ml.linalg.{Vector => MLVector}
import ExtractionAlgorithm._

case class DescriptorFactory (algorithm : ExtractionAlgorithm, featuresCount : Int = defaultFeaturesCount, maxImageSize : Int = defaultMaxImageSize) {

	private def this() = {
		this(defaultAlgorithm, defaultFeaturesCount, defaultMaxImageSize)
	}
	
	require(featuresCount >= 1 && featuresCount < 1000)
	require(maxImageSize >= 4 && maxImageSize <= 8192)

	lazy val detector = FeatureExtractor.createDetector(algorithm, featuresCount)

	def describe(width : Int, height : Int, mode : Int, data : Array[Byte]): Seq[MLVector] = {
		val image = ImageReader.read(width, height, mode, data)
		val resizedImage = ImageReader.limitSize(maxImageSize, image)
		FeatureExtractor.describe(resizedImage, detector)
	}

	/*

	import java.io.{IOException, ObjectInputStream, ObjectOutputStream}

	@throws(classOf[IOException]) private def writeObject(out: ObjectOutputStream): Unit = {
		out.writeInt(algorithm.id)
		out.writeInt(featuresCount)
		out.writeInt(maxImageSize)
	}

	@throws(classOf[IOException]) private def readResolve(in: ObjectInputStream): Object = {
		val algorithm = in.readInt
		val featuresCount = in.readInt
		val maxImageSize = in.readInt
		DescriptorFactory(ExtractionAlgorithm(algorithm), featuresCount, maxImageSize)
	}

	*/

}

object DescriptorFactory {

	val defaultFeaturesCount : Int = 10
	val defaultMaxImageSize : Int = 512
	val defaultAlgorithm : ExtractionAlgorithm = ExtractionAlgorithm.SIFT

}