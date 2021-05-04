package image_classifier.input

import org.apache.spark.sql.SparkSession
import org.apache.spark.rdd.RDD
import org.apache.spark.ml.image.ImageSchema

private[input] case class InputClass(name : String, trainFiles : List[String], testFiles : List[String], mergedFiles : List[String]) {

	require(name.trim.nonEmpty && name.length < 128)

	def load(sparkContext : SparkSession) : RDD[ImageSchema] = ???

}
