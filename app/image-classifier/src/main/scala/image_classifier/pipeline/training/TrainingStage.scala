package image_classifier.pipeline.training

import com.github.dwickern.macros.NameOf.nameOf
import image_classifier.configuration.{Loader, TrainingAlgorithm, TrainingConfig}
import image_classifier.pipeline.Columns.colName
import image_classifier.pipeline.LoaderStage
import image_classifier.pipeline.featurization.FeaturizationStage
import image_classifier.pipeline.training.TrainingStage._
import image_classifier.utils.DataTypeImplicits.DataTypeExtension
import image_classifier.utils.FileUtils
import org.apache.log4j.Logger
import org.apache.spark.ml.classification.{Classifier, _}
import org.apache.spark.ml.linalg.SQLDataTypes.VectorType
import org.apache.spark.ml.linalg.{Vector => MLVector}
import org.apache.spark.ml.param.shared.{HasFeaturesCol, HasLabelCol, HasPredictionCol}
import org.apache.spark.ml.util.{MLReadable, MLWritable}
import org.apache.spark.ml.{Estimator, Model}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.{col, countDistinct}
import org.apache.spark.sql.types.{BooleanType, DataType, IntegerType}

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
		}
		model.read.load(FileUtils.resolve(file, dataPath)).asInstanceOf[ModelType]
	}

	override protected def make(): ModelType = {
		validate(featurizationStage.result.cache.schema)
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
				val featureSize = if (featurizationStage.wasMade)
					featurizationStage.config.codebook.size
				else
					featurizationStage.result.first.getAs[MLVector](featuresCol).size
				val labelsCount = if (featurizationStage.wasMade && featurizationStage.dataStage.wasMade)
					featurizationStage.dataStage.config.labelsCount
				else if (config.labelsCount.isDefined)
					config.labelsCount.get
				else {
					logger.warn(s"Computing labels count. This may take a while. Please consider specifying ${nameOf(config.labelsCount)}.")
					val count = featurizationStage.result.select(countDistinct(labelCol)).first.getLong(0).toInt
					logger.info(s"Labels count is $count")
					count
				}
				require(config.labelsCount.forall(_ == labelsCount), s"${nameOf(config.labelsCount)} does not match labels count")
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

	private def validate(schema: DataType): Unit = {
		schema.requireField(featurizationStage.outputCol, VectorType)
		schema.requireField(featurizationStage.dataStage.isTestCol, BooleanType)
		schema.requireField(featurizationStage.dataStage.labelCol, IntegerType)
	}

	override protected def save(result: ModelType): Unit = {
		fileUtils.makeDirs(file)
		fileUtils.writeBytes(FileUtils.resolve(file, algorithmPath), Array[Byte](config.algorithm.id.toByte))
		result.write.overwrite().save(FileUtils.resolve(file, dataPath))
	}

}

private[pipeline] object TrainingStage {

	private type ModelType = Model[_] with MLWritable
	private type ClassifierType = Classifier[MLVector, _, _ <: MLWritable] with HasLabelCol with HasFeaturesCol with HasPredictionCol

	val defaultPredictionCol: String = colName("prediction")

	private val logger: Logger = Logger.getLogger(getClass)

	private val algorithmPath: String = "algorithm"
	private val dataPath: String = "data"

}