package image_classifier.pipeline.testing

import image_classifier.configuration.TestingConfig
import image_classifier.pipeline.Stage
import image_classifier.pipeline.featurization.FeaturizationStage
import image_classifier.pipeline.training.TrainingStage
import org.apache.spark.sql.SparkSession

private[pipeline] final class TestingStage(config: Option[TestingConfig], val trainingStage: TrainingStage)(implicit spark: SparkSession) extends Stage[Unit, TestingConfig]("Testing", config) {

	override protected def run(specs: TestingConfig): Unit = {
		import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator
		import org.apache.spark.sql.functions.col
		val model = trainingStage.result
		val featurizationStage = trainingStage.featurizationStage
		val dataStage = featurizationStage.dataStage
		val test = featurizationStage.result.filter(col(dataStage.isTestCol))
		val data = model.transform(test)
		val evaluator = new MulticlassClassificationEvaluator()
			.setLabelCol(dataStage.labelCol)
			.setPredictionCol(trainingStage.predictionCol)
			.setMetricName("accuracy")
		println(s"Accuracy is ${evaluator.evaluate(data) * 100}%")
	}

}
