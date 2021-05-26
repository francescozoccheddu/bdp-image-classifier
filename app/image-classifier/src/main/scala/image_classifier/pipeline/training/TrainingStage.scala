package image_classifier.pipeline.training

import image_classifier.configuration.{Loader, TrainingConfig}
import image_classifier.pipeline.LoaderStage
import image_classifier.pipeline.featurization.FeaturizationStage
import image_classifier.pipeline.training.TrainingStage.{ModelType, defaultPredictionCol}
import org.apache.spark.ml.Model
import org.apache.spark.ml.util.MLWritable
import org.apache.spark.sql.SparkSession

private[pipeline] final class TrainingStage(loader: Option[Loader[TrainingConfig]], val featurizationStage: FeaturizationStage, val predictionCol: String = defaultPredictionCol)(implicit spark: SparkSession) extends LoaderStage[ModelType, TrainingConfig]("Training", loader) {

	override protected def load(file: String): ModelType = {
		import image_classifier.configuration.TrainingAlgorithm
		import image_classifier.pipeline.training.TrainingStage.{algorithmPath, dataPath}
		import org.apache.spark.ml.classification.{DecisionTreeClassificationModel, FMClassificationModel, GBTClassificationModel, LinearSVCModel, LogisticRegressionModel, MultilayerPerceptronClassificationModel, NaiveBayesModel, RandomForestClassificationModel}
		import org.apache.spark.ml.util.MLReadable

		import java.nio.file.{Files, Paths}
		val dir = Paths.get(file)
		val bytes = Files.readAllBytes(dir.resolve(algorithmPath))
		require(bytes.length == 1)
		val model: MLReadable[_] = TrainingAlgorithm(bytes(0)) match {
			case TrainingAlgorithm.MultilayerPerceptron => MultilayerPerceptronClassificationModel
			case TrainingAlgorithm.RandomForest => RandomForestClassificationModel
			case TrainingAlgorithm.LinearSupportVector => LinearSVCModel
			case TrainingAlgorithm.GradientBoosted => GBTClassificationModel
			case TrainingAlgorithm.FactorizationMachines => FMClassificationModel
			case TrainingAlgorithm.DecisionTree => DecisionTreeClassificationModel
			case TrainingAlgorithm.LogisticRegression => LogisticRegressionModel
			case TrainingAlgorithm.NaiveBayes => NaiveBayesModel
		}
		model.read.load(dir.resolve(dataPath).toString).asInstanceOf[ModelType]
	}

	override protected def make(config: TrainingConfig): ModelType = {
		import image_classifier.configuration.TrainingAlgorithm
		import image_classifier.pipeline.training.TrainingStage.{ClassifierType, logger}
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
		logger.info(s"Training with '${classifier.getClass.getSimpleName}'")
		val training = featurizationStage.result.filter(!col(featurizationStage.dataStage.isTestCol))
		classifier.setFeaturesCol(featuresCol)
		classifier.setLabelCol(labelCol)
		classifier.setPredictionCol(predictionCol)
		classifier.fit(training).asInstanceOf[ModelType]
	}

	override protected def save(result: ModelType, file: String): Unit = {
		import image_classifier.pipeline.training.TrainingStage.{algorithmPath, dataPath}
		import java.nio.file.{Files, Paths}
		val dir = Paths.get(file)
		Files.createDirectories(dir)
		Files.write(dir.resolve(algorithmPath), Array[Byte](specs.get.config.get.algorithm.id.toByte))
		result.write.save(dir.resolve(dataPath).toString)
	}

}

private[pipeline] object TrainingStage {
	import image_classifier.pipeline.utils.Columns.colName
	import org.apache.log4j.Logger
	import org.apache.spark.ml.classification.Classifier
	import org.apache.spark.ml.param.shared.{HasFeaturesCol, HasLabelCol, HasPredictionCol}
	import org.apache.spark.ml.linalg.{Vector => MLVector}

	private type ModelType = Model[_] with MLWritable
	private type ClassifierType = Classifier[MLVector, _, _ <: MLWritable] with HasLabelCol with HasFeaturesCol with HasPredictionCol

	val defaultPredictionCol = colName("prediction")

	private val logger = Logger.getLogger(getClass)

	private val algorithmPath = "algorithm"
	private val dataPath = "data"

}