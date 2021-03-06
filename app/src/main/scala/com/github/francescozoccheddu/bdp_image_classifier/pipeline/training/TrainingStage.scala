package com.github.francescozoccheddu.bdp_image_classifier.pipeline.training

import com.github.francescozoccheddu.bdp_image_classifier.configuration.{Loader, TrainingAlgorithm, TrainingConfig}
import com.github.francescozoccheddu.bdp_image_classifier.pipeline.LoaderStage
import com.github.francescozoccheddu.bdp_image_classifier.pipeline.featurization.FeaturizationStage
import com.github.francescozoccheddu.bdp_image_classifier.pipeline.training.TrainingStage._
import com.github.francescozoccheddu.bdp_image_classifier.pipeline.utils.Columns.colName
import com.github.francescozoccheddu.bdp_image_classifier.utils.FileUtils
import org.apache.log4j.Logger
import org.apache.spark.ml.classification.{Classifier, _}
import org.apache.spark.ml.linalg.Vector
import org.apache.spark.ml.param.shared.{HasFeaturesCol, HasLabelCol, HasPredictionCol}
import org.apache.spark.ml.util.{MLReadable, MLWritable}
import org.apache.spark.ml.{Estimator, Model}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.col

private[pipeline] final class TrainingStage(loader: Option[Loader[TrainingConfig]], val featurizationStage: FeaturizationStage, val predictionCol: String = defaultPredictionCol)(implicit spark: SparkSession, fileUtils: FileUtils)
  extends LoaderStage[ModelType, TrainingConfig]("Training", loader)(fileUtils) {

	override protected def load(): ModelType = {
		val bytes = fileUtils.readBytes(FileUtils.resolve(file, algorithmPath))
		require(bytes.length == 1)
		val model: MLReadable[_] = TrainingAlgorithm(bytes(0)) match {
			case TrainingAlgorithm.MultilayerPerceptron => MultilayerPerceptronClassificationModel
			case TrainingAlgorithm.RandomForest => RandomForestClassificationModel
			case TrainingAlgorithm.LinearSupportVector => OneVsRestModel
			case TrainingAlgorithm.GradientBoosted => GBTClassificationModel
			case TrainingAlgorithm.FactorizationMachines => FMClassificationModel
			case TrainingAlgorithm.DecisionTree => DecisionTreeClassificationModel
			case TrainingAlgorithm.LogisticRegression => LogisticRegressionModel
			case TrainingAlgorithm.NaiveBayes => NaiveBayesModel
			case TrainingAlgorithm.NearestNeighbor => NaiveNearestNeighborModel
		}
		model.read.load(FileUtils.resolve(file, dataPath)).asInstanceOf[ModelType]
	}

	override protected def make(): ModelType = {
		val (featuresCol, labelCol) = (featurizationStage.outputCol, featurizationStage.dataStage.labelCol)
		val classifier: ClassifierType = config.algorithm match {
			case TrainingAlgorithm.NaiveBayes =>
				new NaiveBayes()
			case TrainingAlgorithm.LogisticRegression =>
				new LogisticRegression()
				  .setElasticNetParam(config.elasticNetParam)
				  .setRegParam(config.regParam)
				  .setMaxIter(config.maxIterations)
				  .setTol(config.convergenceTolerance)
				  .setAggregationDepth(config.depth)
			case TrainingAlgorithm.DecisionTree =>
				new DecisionTreeClassifier()
				  .setSeed(config.actualSeed)
				  .setMaxBins(config.maxBins)
				  .setMaxDepth(config.depth)
				  .setMinInfoGain(config.minInfoGain)
			case TrainingAlgorithm.FactorizationMachines =>
				new FMClassifier()
				  .setMaxIter(config.maxIterations)
				  .setRegParam(config.regParam)
				  .setFactorSize(config.factorSize)
				  .setSeed(config.actualSeed)
				  .setStepSize(config.stepSize)
				  .setTol(config.convergenceTolerance)
			case TrainingAlgorithm.GradientBoosted =>
				new GBTClassifier()
				  .setMaxIter(config.maxIterations)
				  .setStepSize(config.stepSize)
				  .setMaxBins(config.maxBins)
				  .setMaxDepth(config.depth)
				  .setSeed(config.actualSeed)
				  .setMinInfoGain(config.minInfoGain)
			case TrainingAlgorithm.LinearSupportVector =>
				new LinearSVC()
				  .setMaxIter(config.maxIterations)
				  .setRegParam(config.regParam)
				  .setAggregationDepth(config.depth)
				  .setTol(config.convergenceTolerance)
			case TrainingAlgorithm.RandomForest =>
				new RandomForestClassifier()
				  .setNumTrees(config.treeCount)
				  .setSeed(config.actualSeed)
				  .setMaxBins(config.maxBins)
				  .setMaxDepth(config.depth)
				  .setMinInfoGain(config.minInfoGain)
			case TrainingAlgorithm.MultilayerPerceptron =>
				new MultilayerPerceptronClassifier()
				  .setSeed(config.actualSeed)
				  .setMaxIter(config.maxIterations)
				  .setLayers(featurizationStage.codebookSize +: config.hiddenLayers.toArray :+ featurizationStage.labelsCount)
				  .setStepSize(config.stepSize)
				  .setTol(config.convergenceTolerance)
			case TrainingAlgorithm.NearestNeighbor =>
				new NaiveNearestNeighbor()
		}
		val training = featurizationStage
		  .result
		  .filter(!col(featurizationStage.dataStage.isTestCol))
		  .repartition(spark.sparkContext.defaultParallelism)
		logger.info(s"Training with '${classifier.getClass.getSimpleName}'")
		classifier.setFeaturesCol(featuresCol)
		classifier.setLabelCol(labelCol)
		classifier.setPredictionCol(predictionCol)
		val estimator: Estimator[_ <: MLWritable] = config.algorithm match {
			case TrainingAlgorithm.LinearSupportVector =>
				new OneVsRest()
				  .setClassifier(classifier)
				  .setFeaturesCol(featuresCol)
				  .setLabelCol(labelCol)
				  .setPredictionCol(predictionCol)
			case _ => classifier
		}
		estimator.fit(training).asInstanceOf[ModelType]
	}

	override protected def save(result: ModelType): Unit = {
		fileUtils.makeDirs(file)
		fileUtils.writeBytes(FileUtils.resolve(file, algorithmPath), Array[Byte](config.algorithm.id.toByte))
		result.write.overwrite().save(FileUtils.resolve(file, dataPath))
	}

}

private[pipeline] object TrainingStage {

	private type ModelType = Model[_] with MLWritable
	private type ClassifierType = Classifier[Vector, _, _ <: MLWritable] with HasLabelCol with HasFeaturesCol with HasPredictionCol

	val defaultPredictionCol: String = colName("prediction")

	private val logger: Logger = Logger.getLogger(getClass)

	private val algorithmPath: String = "algorithm"
	private val dataPath: String = "data"

}