package image_classifier.input

import org.apache.spark.sql.SparkSession
import org.apache.spark.rdd.RDD
import org.apache.spark.ml.image.ImageSchema

private[input] case class InputClass(name : String, trainFiles : Seq[String] = Seq[String](), testFiles : Seq[String] = Seq[String](), mergedFiles : Seq[String] = Seq[String]()) {

	require(name.trim.nonEmpty && name.length < 128)

}
