package image_classifier.configuration

import scala.util.Random
import com.github.dwickern.macros.NameOf._
import image_classifier.configuration
import image_classifier.configuration.ImageFeatureAlgorithm.ImageFeatureAlgorithm
import image_classifier.configuration.TrainingAlgorithm.TrainingAlgorithm
import image_classifier.configuration.Utils._
import image_classifier.pipeline.featurization.DescriptorConfig
import image_classifier.utils.FileUtils
import org.apache.log4j.Logger

private[image_classifier] sealed trait LoadableConfig

final case class DataConfig private[configuration](
                                                    tempFile: String = DataConfig.defaultTempFile,
                                                    dataSet: O[Seq[Seq[String]]] = None,
                                                    trainingSet: O[Seq[Seq[String]]] = None,
                                                    testSet: O[Seq[Seq[String]]] = None,
                                                    testFraction: Double = JointDataConfig.defaultTestFraction,
                                                    splitSeed: Int = JointDataConfig.defaultSplitSeed,
                                                    stratified: Boolean = JointDataConfig.defaultStratified
                                                  ) extends LoadableConfig {

	require(testFraction >= 0 && testFraction <= 1, s"${nameOf(testFraction)} must fall in range [0, 1]")
	require(FileUtils.isValidPath(tempFile), s"${nameOf(tempFile)} is not a valid file path")

}

object DataConfig {

	val defaultTempFile: String = "hdfs://localhost:9000/ic_temp_data"

}

object JointDataConfig {

	val defaultStratified: Boolean = true
	val defaultTestFraction: Double = 0.2

	def defaultSplitSeed: Int = util.Random.nextInt

}

final case class FeaturizationConfig(
                                      codebookSize: Int = FeaturizationConfig.defaultCodebookSize,
                                      override val algorithm: ImageFeatureAlgorithm = FeaturizationConfig.defaultAlgorithm,
                                      override val featureCount: Int = FeaturizationConfig.defaultFeatureCount,
                                      override val octaveLayerCount: Int = FeaturizationConfig.defaultOctaveLayerCount,
                                      override val contrastThreshold: Double = FeaturizationConfig.defaultContrastThreshold,
                                      override val edgeThreshold: Double = FeaturizationConfig.defaultEdgeThreshold,
                                      override val sigma: Double = FeaturizationConfig.defaultSigma,
                                      maxSize: Int = FeaturizationConfig.defaultMaxSize,
                                      assignNearest: Boolean = FeaturizationConfig.defaultAssignNearest
                                    ) extends LoadableConfig with DescriptorConfig {

	require(codebookSize >= 1 && codebookSize <= 10000, s"${nameOf(codebookSize)} must fall in range [1, 10000]")
	require(featureCount >= 1 && featureCount <= 1000, s"${nameOf(featureCount)} must fall in range [1, 1000]")
	require(maxSize >= 4 && maxSize <= 8192, s"${nameOf(maxSize)} must fall in range [4, 8192]")
	require(octaveLayerCount >= 1 && octaveLayerCount <= 32, s"${nameOf(octaveLayerCount)} must fall in range [1, 32]")
	require(contrastThreshold >= 0, s"${nameOf(contrastThreshold)} cannot be negative")
	require(edgeThreshold >= 0, s"${nameOf(edgeThreshold)} cannot be negative")
	require(sigma >= 0, s"${nameOf(sigma)} cannot be negative")

}

object FeaturizationConfig {

	val defaultCodebookSize: Int = 500
	val defaultAlgorithm: configuration.ImageFeatureAlgorithm.Value = ImageFeatureAlgorithm.Sift
	val defaultOctaveLayerCount: Int = 3
	val defaultContrastThreshold: Double = 0.04
	val defaultEdgeThreshold: Int = 10
	val defaultSigma: Double = 1.6
	val defaultFeatureCount: Int = 20
	val defaultMaxSize: Int = 1024
	val defaultAssignNearest: Boolean = true

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
                                 seed: Int = TrainingConfig.defaultSeed
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

	val defaultAlgorithm: configuration.TrainingAlgorithm.Value = TrainingAlgorithm.NaiveBayes
	val defaultMaxIterations: Int = 10
	val defaultRegParam: Double = 0.3
	val defaultElasticNetParam: Double = 0.8
	val defaultTreeCount: Int = 4
	val defaultHiddenLayers: Seq[Int] = Seq.empty
	val defaultBlockSize: Int = 128
	val defaultStepSize: Double = 0.001
	val defaultSeed: Int = Random.nextInt

}

final case class TestingConfig(
                                save: O[String] = None,
                                labels: O[Seq[String]] = None
                              ) {

	require(save.forall(FileUtils.isValidPath), s"${nameOf(save)} is not a valid file path")

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

	def saveLocal(file: String): Unit = configToFile(this, file)

	def save(file: String)(implicit fileUtils: FileUtils): Unit = configToFile(this, file, fileUtils)

	def toJson: String = configToJson(this)

}

object Config {

	private val logger: Logger = Logger.getLogger(getClass)

	def fromLocalFile(file: String): Config = {
		logger.info(s"Loading config file '$file'")
		configFromFile(file)
	}

	def fromFile(file: String)(implicit fileUtils: FileUtils): Config = {
		logger.info(s"Loading config file '$file'")
		configFromFile(file, fileUtils)
	}

	def fromJson(json: String): Config =
		configFromJson(json)

}


