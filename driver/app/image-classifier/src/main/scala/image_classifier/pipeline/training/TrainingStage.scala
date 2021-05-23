package image_classifier.pipeline.training

import image_classifier.configuration.{Loader, TrainingConfig}
import image_classifier.pipeline.LoaderStage
import image_classifier.pipeline.featurization.FeaturizationStage
import image_classifier.pipeline.training.TrainingStage.{ModelType, defaultPredictionCol}
import org.apache.spark.ml.Model
import org.apache.spark.ml.util.MLWritable
import org.apache.spark.sql.SparkSession

private[pipeline] final class TrainingStage(loader: Option[Loader[TrainingConfig]], val featurizationStage: FeaturizationStage, val predictionCol: String = defaultPredictionCol)(implicit spark: SparkSession) extends LoaderStage[ModelType, TrainingConfig]("Training", loader) {

	override protected def load(file: String): ModelType = ???

	override protected def make(config: TrainingConfig): ModelType = {
		import image_classifier.configuration.TrainingAlgorithm
		import image_classifier.pipeline.training.TrainingStage.ClassifierType
		import org.apache.spark.ml.classification.{DecisionTreeClassifier, FMClassifier, GBTClassifier, LinearSVC, LogisticRegression, MultilayerPerceptronClassifier, NaiveBayes, RandomForestClassifier}
		import org.apache.spark.sql.functions.col
		featurizationStage.result.cache
		val (featuresCol, labelCol) = (featurizationStage.outputCol, featurizationStage.dataStage.labelCol)
		val classifier: ClassifierType = config.algorithm match {
			case TrainingAlgorithm.NaiveBayes =>
				new NaiveBayes()
			case TrainingAlgorithm.LogisticRegression =>
				new LogisticRegression()
					.setElasticNetParam(config.elasticNetParam)
					.setRegParam(config.regParam)
					.setMaxIter(config.maxIterations)
			case TrainingAlgorithm.DecisionTree =>
				new DecisionTreeClassifier()
					.setSeed(config.seed)
			case TrainingAlgorithm.FactorizationMachines =>
				new FMClassifier()
					.setMaxIter(config.maxIterations)
					.setRegParam(config.regParam)
			case TrainingAlgorithm.GradientBoosted =>
				new GBTClassifier()
					.setMaxIter(config.maxIterations)
					.setStepSize(config.stepSize)
			case TrainingAlgorithm.LinearSupportVector =>
				new LinearSVC()
					.setMaxIter(config.maxIterations)
					.setRegParam(config.regParam)
			case TrainingAlgorithm.RandomForest =>
				new RandomForestClassifier()
					.setNumTrees(config.treeCount)
					.setSeed(config.seed)
			case TrainingAlgorithm.MultilayerPerceptron =>
				import image_classifier.configuration.LoadMode
				import org.apache.spark.ml.linalg.{Vector => MLVector}
				import org.apache.spark.sql.functions.approx_count_distinct
				val featureSize = featurizationStage.specs.get.mode match {
					case LoadMode.Make | LoadMode.MakeAndSave => featurizationStage.specs.get.config.get.codebookSize
					case _ => featurizationStage.result.first.getAs[MLVector](featuresCol).size
				}
				val labelsCount = featurizationStage.result.select(approx_count_distinct(labelCol)).first.getLong(0).toInt
				new MultilayerPerceptronClassifier()
					.setSeed(config.seed)
					.setMaxIter(config.maxIterations)
					.setLayers(featureSize +: config.hiddenLayers.toArray :+ labelsCount)
					.setStepSize(config.stepSize)
					.setBlockSize(config.blockSize)
		}
		val training = featurizationStage.result.filter(!col(featurizationStage.dataStage.isTestCol))
		classifier.setFeaturesCol(featuresCol)
		classifier.setLabelCol(labelCol)
		classifier.setPredictionCol(predictionCol)
		classifier.fit(training).asInstanceOf[ModelType]
	}

	override protected def save(result: ModelType, file: String): Unit = ???

}

private[pipeline] object TrainingStage {
	import image_classifier.pipeline.utils.Columns.colName
	import org.apache.spark.ml.classification.Classifier
	import org.apache.spark.ml.param.shared.{HasFeaturesCol, HasLabelCol, HasPredictionCol}
	import org.apache.spark.ml.linalg.{Vector => MLVector}

	private type ModelType = Model[_] with MLWritable
	private type ClassifierType = Classifier[MLVector, _, _ <: MLWritable] with HasLabelCol with HasFeaturesCol with HasPredictionCol

	val defaultPredictionCol = colName("prediction")

	private val algorithmPath = "algorithm"
	private val dataPath = "algorithm"

}