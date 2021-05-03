package com.fra

import org.bytedeco.javacpp.opencv_xfeatures2d.SIFT
import DescriptorFactory.{defaultContrastThreshold, defaultEdgeThreshold, defaultFeaturesCount, defaultOctavesCount, defaultSigma}

class DescriptorFactory private(private val sift: SIFT, val featuresCount: Int, val octavesCount: Int, val contrastThreshold: Double, val edgeThreshold: Double, val sigma: Double) extends Serializable {

	def this(featuresCount: Int = defaultFeaturesCount, octavesCount: Int = defaultOctavesCount, contrastThreshold: Double = defaultContrastThreshold, edgeThreshold: Double = defaultEdgeThreshold, sigma: Double = defaultSigma) = {
		this(SIFT.create(featuresCount, octavesCount, contrastThreshold, edgeThreshold, sigma), featuresCount, octavesCount, contrastThreshold, edgeThreshold, sigma)
	}

	private def this() = {
		this(null, defaultFeaturesCount, defaultOctavesCount, defaultContrastThreshold, defaultEdgeThreshold, defaultSigma)
	}

	import org.apache.spark.mllib.linalg

	import java.io.{IOException, ObjectInputStream, ObjectOutputStream}

	require(featuresCount > 1 && featuresCount < 1000)
	require(octavesCount > 1 && octavesCount < 10)
	require(contrastThreshold > 0)
	require(edgeThreshold > 0)
	require(sigma > 1)

	def describe(imageFile: String): IndexedSeq[linalg.Vector] = {
		import DescriptorFactory.siftSize
		import org.bytedeco.javacpp.opencv_core.{CvType, KeyPointVector, Mat}
		import org.bytedeco.javacpp.opencv_imgcodecs.imread
		import org.apache.spark.mllib.linalg.Vectors
		val img = imread(imageFile)
		val kp = new KeyPointVector
		sift.detect(img, kp)
		val kpCount = kp.size.toInt
		val desMat = new Mat(kpCount, siftSize)
		sift.compute(img, kp, desMat)
		for (d <- 0 until kpCount) yield {
			val desRow = Array.ofDim[Double](siftSize)
			val row = desMat.row(d).data
			for (i <- 0 until siftSize) desRow(i) = row.getDouble(i)
			Vectors.dense(desRow)
		}
	}

	@throws(classOf[IOException]) private def writeObject(out: ObjectOutputStream): Unit = {
		out.writeInt(featuresCount)
		out.writeInt(octavesCount)
		out.writeDouble(contrastThreshold)
		out.writeDouble(edgeThreshold)
		out.writeDouble(sigma)
	}

	@throws(classOf[IOException]) private def readResolve(in: ObjectInputStream): Object = {
		val featuresCount = in.readInt
		val octavesCount = in.readInt
		val contrastThreshold = in.readDouble
		val edgeThreshold = in.readDouble
		val sigma = in.readDouble
		new DescriptorFactory(featuresCount, octavesCount, contrastThreshold, edgeThreshold, sigma)
	}

}

object DescriptorFactory {

	private def siftSize: Int = 128

	val defaultFeaturesCount: Int = 10
	val defaultOctavesCount: Int = 3
	val defaultContrastThreshold: Double = 0.04
	val defaultEdgeThreshold: Double = 10
	val defaultSigma: Double = 1.6

}