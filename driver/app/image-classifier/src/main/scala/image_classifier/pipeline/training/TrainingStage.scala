package image_classifier.pipeline.training

import image_classifier.configuration.{Loader, TrainingConfig}
import image_classifier.pipeline.LoaderStage
import image_classifier.pipeline.featurization.FeaturizationStage
import org.apache.spark.ml.Model
import org.apache.spark.sql.SparkSession

private[pipeline] final class TrainingStage(loader: Option[Loader[TrainingConfig]], val featurizationStage: FeaturizationStage)(implicit spark: SparkSession) extends LoaderStage[Model[_], TrainingConfig]("Training", loader) {

	override protected def load(file: String): Model[_] = ???

	override protected def make(config: TrainingConfig): Model[_] = ???

	override protected def save(result: Model[_], file: String): Unit = ???

}

