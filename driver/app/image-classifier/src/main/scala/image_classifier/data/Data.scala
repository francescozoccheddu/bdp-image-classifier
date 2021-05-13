package image_classifier.data

import org.apache.spark.sql.DataFrame

case class Data private[data](trainingSet: DataFrame, testSet: DataFrame) {

	def hasTrainingSet = trainingSet != null
	def hasTestSet = testSet != null

}
