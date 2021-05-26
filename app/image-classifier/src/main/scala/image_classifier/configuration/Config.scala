package image_classifier.configuration

import com.github.dwickern.macros.NameOf._
import image_classifier.configuration.ImageFeatureAlgorithm.ImageFeatureAlgorithm
import image_classifier.configuration.TrainingAlgorithm.TrainingAlgorithm
import image_classifier.configuration.Utils._

private[image_classifier] sealed trait LoadableConfig

private[image_classifier] sealed trait HdfsLoadableConfig extends LoadableConfig

final case class DataConfig private[configuration](
	tempFile: String = DataConfig.defaultTempFile,
	dataSet: O[Seq[Seq[String]]] = None,
	trainingSet: O[Seq[Seq[String]]] = None,
	testSet: O[Seq[Seq[String]]] = None,
	testFraction: Double = JointDataConfig.defaultTestFraction,
	splitSeed: Int = JointDataConfig.defaultSplitSeed,
	stratified: Boolean = JointDataConfig.defaultStratified
) extends HdfsLoadableConfig {

	require(testFraction >= 0 && testFraction <= 1, s"${nameOf(testFraction)} must fall in range [0, 1]")
	require(isValidHdfsFilePath(tempFile), s"${nameOf(tempFile)} is not a valid HDFS file path")

}

object DataConfig {

	val defaultTempFile = "hdfs://image_classifier_temp"

}

object JointDataConfig {

	val defaultTestFraction = 0.2
	val defaultStratified = true
	def defaultSplitSeed = util.Random.nextInt

}

final case class FeaturizationConfig(
	codebookSize: Int = FeaturizationConfig.defaultCodebookSize,
	featureCount: Int = FeaturizationConfig.defaultFeatureCount,
	algorithm: ImageFeatureAlgorithm = FeaturizationConfig.defaultAlgorithm,
	maxSize: Int = FeaturizationConfig.defaultMaxSize,
	assignNearest: Boolean = FeaturizationConfig.defaultAssignNearest
) extends LoadableConfig {

	require(codebookSize >= 1 && codebookSize <= 10000, s"${nameOf(codebookSize)} must fall in range [1, 10000]")
	require(featureCount >= 1 && featureCount <= 1000, s"${nameOf(featureCount)} must fall in range [1, 1000]")
	require(maxSize >= 4 && maxSize <= 8192, s"${nameOf(maxSize)} must fall in range [4, 8192]")

}

object FeaturizationConfig {

	val defaultCodebookSize = 500
	val defaultAlgorithm = ImageFeatureAlgorithm.Sift
	val defaultFeatureCount = 10
	val defaultMaxSize = 512
	val defaultAssignNearest = true

}

final case class TrainingConfig(
	algorithm: TrainingAlgorithm = TrainingConfig.defaultAlgorithm,
	maxIterations: Int = TrainingConfig.defaultMaxIterations,
	regParam: Double = TrainingConfig.defaultRegParam,
	elasticNetParam: Double = TrainingConfig.defaultElasticNetParam,
	treeCount: Int = TrainingConfig.defaultTreeCount,
	hiddenLayers: Seq[Int] = TrainingConfig.defaultHiddenLayers,
	blockSize: Int = TrainingConfig.defaultBlockSize,
	stepSize: Double = TrainingConfig.defaultStepSize,
	seed: Int = TrainingConfig.defaultSeed,
) extends LoadableConfig {

	require(maxIterations > 0, s"${nameOf(maxIterations)} must be positive")
	require(blockSize >= 4, s"${nameOf(blockSize)} must be at least 4")
	require(elasticNetParam >= 0 && elasticNetParam <= 1, s"${nameOf(elasticNetParam)} must fall in range [0,1]")
	require(stepSize > 0, s"${nameOf(stepSize)} must be positive")
	require(regParam >= 0, s"${nameOf(regParam)} cannot be negative")
	require(treeCount >= 0, s"${nameOf(treeCount)} cannot be negative")
	require(hiddenLayers.forall(_ > 0), s"${nameOf(hiddenLayers)} must be positive")

}

object TrainingConfig {
	import scala.util.Random

	val defaultAlgorithm = TrainingAlgorithm.NaiveBayes
	val defaultMaxIterations = 10
	val defaultRegParam = 0.3
	val defaultElasticNetParam = 0.8
	val defaultTreeCount = 4
	val defaultHiddenLayers = Seq.empty[Int]
	val defaultBlockSize = 128
	val defaultStepSize = 0.001
	val defaultSeed = Random.nextInt

}

final case class TestingConfig(
	save: O[String] = None,
	labels: O[Seq[String]] = None
) {

	require(save.forall(isValidFilePath), s"${nameOf(save)} is not a valid file path")

}

final case class Config(
	data: OL[DataConfig],
	featurization: OL[FeaturizationConfig] = None,
	training: OL[TrainingConfig] = None,
	testing: O[TestingConfig] = None
) {

	require(featurization.forall(_.config.isEmpty) || data.isDefined, s"${nameOf(featurization)} requires ${nameOf(data)}")
	require(training.forall(_.config.isEmpty) || featurization.isDefined, s"${nameOf(training)} requires ${nameOf(featurization)}")
	require(testing.isEmpty || training.isDefined, s"${nameOf(testing)} requires ${nameOf(training)}")
	require(testing.isEmpty || featurization.isDefined, s"${nameOf(testing)} requires ${nameOf(featurization)}")
	data.foreach(_.requireValidPaths)
	featurization.foreach(_.requireValidPaths)
	training.foreach(_.requireValidPaths)

	def save(file: String) = configToFile(this, file)
	def toJson = configToJson(this)

}

object Config {
	import org.apache.log4j.Logger

	private val logger = Logger.getLogger(getClass)

	def fromFile(file: String) = {
		logger.info(s"Loading config file '$file'")
		configFromFile(file)
	}

	def fromJson(json: String) =
		configFromJson(json)

}


