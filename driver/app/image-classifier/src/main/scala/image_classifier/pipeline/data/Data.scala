package image_classifier.pipeline.data

import org.apache.spark.sql.DataFrame

private[pipeline] case class Data private[data](trainingSet: DataFrame, testSet: DataFrame) {

	def hasTrainingSet = trainingSet != null
	def hasTestSet = testSet != null

}
