package image_classifier.pipeline.training

import image_classifier.configuration.{Loader, TrainingConfig}
import image_classifier.pipeline.LoaderStage
import image_classifier.pipeline.featurization.FeaturizationStage
import image_classifier.pipeline.training.TrainingStage.defaultPredictionCol
import org.apache.spark.ml.Model
import org.apache.spark.sql.SparkSession

private[pipeline] final class TrainingStage(loader: Option[Loader[TrainingConfig]], val featurizationStage: FeaturizationStage, val predictionCol: String = defaultPredictionCol)(implicit spark: SparkSession) extends LoaderStage[Model[_], TrainingConfig]("Training", loader) {

	override protected def load(file: String): Model[_] = ???

	override protected def make(config: TrainingConfig): Model[_] = {
		import image_classifier.configuration.TrainingAlgorithm
		import org.apache.spark.ml.classification.{LogisticRegression, NaiveBayes, DecisionTreeClassifier}
		import org.apache.spark.sql.functions.col
		featurizationStage.result.cache
		val (featuresCol, labelCol) = (featurizationStage.outputCol, featurizationStage.dataStage.labelCol)
		val training = featurizationStage.result.filter(!col(featurizationStage.dataStage.isTestCol))
		config.algorithm match {
			case TrainingAlgorithm.NaiveBayes =>
				new NaiveBayes()
					.setFeaturesCol(featuresCol)
					.setLabelCol(labelCol)
					.setPredictionCol(predictionCol)
					.fit(training)
			case TrainingAlgorithm.LogisticRegression =>
				new LogisticRegression()
					.setFeaturesCol(featuresCol)
					.setLabelCol(labelCol)
					.setPredictionCol(predictionCol)
					.fit(training)
			case TrainingAlgorithm.DecisionTree =>
				new DecisionTreeClassifier()
					.setFeaturesCol(featuresCol)
					.setLabelCol(labelCol)
					.setPredictionCol(predictionCol)
					.fit(training)
		}
	}

	override protected def save(result: Model[_], file: String): Unit = ???

}

private[pipeline] object TrainingStage {
	import image_classifier.pipeline.utils.Columns.colName

	val defaultPredictionCol = colName("prediction")

	private val algorithmPath = "algorithm"
	private val dataPath = "algorithm"

}