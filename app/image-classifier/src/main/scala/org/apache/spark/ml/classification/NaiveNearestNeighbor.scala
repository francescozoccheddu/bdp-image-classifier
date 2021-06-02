package org.apache.spark.ml.classification

import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.ml.linalg.{SparseVector, Vector, Vectors}
import org.apache.spark.ml.param.ParamMap
import org.apache.spark.ml.util._
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.types.IntegerType
import org.apache.spark.sql.{DataFrame, Dataset}

final class NaiveNearestNeighbor(override val uid: String) extends Classifier[Vector, NaiveNearestNeighbor, NaiveNearestNeighborModel] with DefaultParamsWritable {

	def this() = this(Identifiable.randomUID(getClass.getSimpleName))

	override def copy(extra: ParamMap): NaiveNearestNeighbor = defaultCopy(extra)

	override protected def train(dataset: Dataset[_]): NaiveNearestNeighborModel = {
		import dataset.sparkSession.implicits._
		new NaiveNearestNeighborModel(dataset.select(col($(featuresCol)), col($(labelCol)).cast(IntegerType)).as[(Vector, Int)].collect())
	}
}

object NaiveNearestNeighbor extends DefaultParamsReadable[NaiveNearestNeighbor]

private[classification] final class NaiveNearestNeighborModel(override val uid: String, private val trainRaw: Array[(Vector, Int)])
  extends ClassificationModel[Vector, NaiveNearestNeighborModel] with MLWritable {

	@transient override val numFeatures: Int = trainRaw.length
	override val numClasses: Int = trainRaw.map(_._2).distinct.length
	@transient private var context: SparkContext = _
	private var trainFeaturesBroadcast: Broadcast[Array[Vector]] = _
	private var trainLabelsBroadcast: Broadcast[Array[Int]] = _

	def this(trainRaw: Array[(Vector, Int)]) = this(Identifiable.randomUID(getClass.getSimpleName), trainRaw)

	override def copy(extra: ParamMap): NaiveNearestNeighborModel = {
		val copied = new NaiveNearestNeighborModel(uid, trainRaw).setParent(parent)
		copyValues(copied, extra)
	}

	override def transform(dataset: Dataset[_]): DataFrame = {
		create(dataset.sparkSession.sparkContext)
		super.transform(dataset)
	}

	private def create(context: SparkContext): Unit = {
		if (context != this.context) {
			this.context = context
			if (trainFeaturesBroadcast != null) trainFeaturesBroadcast.destroy()
			if (trainLabelsBroadcast != null) trainLabelsBroadcast.destroy()
			trainFeaturesBroadcast = context.broadcast(trainRaw.map(_._1))
			trainLabelsBroadcast = context.broadcast(trainRaw.map(_._2))
		}
	}

	override def predictRaw(features: Vector): Vector =
		Vectors.sparse(numClasses, Array(predictLabel(features)), Array(1.0))

	override def raw2prediction(rawPrediction: Vector): Double = rawPrediction.asInstanceOf[SparseVector].indices(0)

	override def predict(features: Vector): Double = predictLabel(features)

	def predictLabel(features: Vector): Int = {
		var minDist = Double.PositiveInfinity
		var minI = 0
		var i = 0
		for (v <- trainFeaturesBroadcast.value) {
			val dist = Vectors.sqdist(v, features)
			if (dist < minDist) {
				minDist = dist
				minI = i
			}
			i += 1
		}
		trainLabelsBroadcast.value(minI)
	}

	override def write: MLWriter = new NaiveNearestNeighborModel.NaiveNearestNeighborModelWriter(this)

}

object NaiveNearestNeighborModel extends MLReadable[NaiveNearestNeighborModel] {

	private val saveDataPath: String = "data"

	override def read: MLReader[NaiveNearestNeighborModel] = new NaiveNearestNeighborModelReader

	private final class NaiveNearestNeighborModelWriter(instance: NaiveNearestNeighborModel) extends MLWriter {

		override protected def saveImpl(path: String): Unit = {
			DefaultParamsWriter.saveMetadata(instance, path, sc)
			val dataPath = new Path(path, saveDataPath).toString
			sparkSession.createDataFrame(instance.trainRaw).repartition(1).write.parquet(dataPath)
		}
	}

	private class NaiveNearestNeighborModelReader extends MLReader[NaiveNearestNeighborModel] {

		private val className: String = classOf[NaiveNearestNeighborModel].getName

		override def load(path: String): NaiveNearestNeighborModel = {
			val metadata = DefaultParamsReader.loadMetadata(path, sc, className)
			val dataPath = new Path(path, saveDataPath).toString
			val spark = sparkSession
			import spark.implicits._
			val data = sparkSession.read.parquet(dataPath).as[(Vector, Int)].collect
			val model = new NaiveNearestNeighborModel(metadata.uid, data)
			metadata.getAndSetParams(model)
			model
		}
	}

}
