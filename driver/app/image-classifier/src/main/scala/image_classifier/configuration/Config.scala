package image_classifier.configuration

import com.github.dwickern.macros.NameOf._
import image_classifier.configuration.ImageFeatureAlgorithm.ImageFeatureAlgorithm
import image_classifier.configuration.LogLevel.LogLevel
import image_classifier.configuration.TrainingAlgorithm.TrainingAlgorithm
import image_classifier.configuration.Utils._
import scala.reflect.runtime.universe._

private[configuration] sealed trait LoadableConfig

private[configuration] sealed trait HdfsLoadableConfig extends LoadableConfig

sealed trait DataConfig {

	def tempDir: String
	private[configuration] def hasTrainingSet: Boolean
	private[configuration] def hasTestSet: Boolean

	// TODO Uncomment when Hadoop is working
	//require(isValidHdfsFilePath(tempFile), s"${nameOf(tempFile)} is not a valid hdfs file path")

}

object DataConfig {

	val defaultTempDir = "hdfs://._image_classifier_temp"

}

final case class DataSetConfig(
	classFiles: Seq[Seq[String]]
) extends HdfsLoadableConfig

final case class SplitDataConfig(
	override val tempDir: String = DataConfig.defaultTempDir,
	trainingSet: OL[DataSetConfig] = None,
	testSet: OL[DataSetConfig] = None
) extends DataConfig {

	trainingSet.foreach(_.requireValidPaths)
	testSet.foreach(_.requireValidPaths)

	private[configuration] override def hasTrainingSet = trainingSet.isDefined
	private[configuration] override def hasTestSet = testSet.isDefined

}

final case class JointDataConfig(
	override val tempDir: String = DataConfig.defaultTempDir,
	dataSet: L[DataSetConfig],
	testFraction: Double = JointDataConfig.defaultTestFraction,
	splitSeed: Int = JointDataConfig.defaultSplitSeed,
	stratified: Boolean = JointDataConfig.defaultStratified
) extends DataConfig {

	require(testFraction >= 0 && testFraction <= 1, s"${nameOf(testFraction)} must fall in range [0, 1]")
	dataSet.requireValidPaths

	private[configuration] override def hasTrainingSet = testFraction < 1
	private[configuration] override def hasTestSet = testFraction > 0

}

object JointDataConfig {

	val defaultTestFraction = 0.2
	val defaultStratified = true
	def defaultSplitSeed = util.Random.nextInt

}

final case class FeaturizationConfig(
	codebookSize: Int = FeaturizationConfig.defaultCodebookSize,
	featureCount: Int = FeaturizationConfig.defaultFeatureCount,
	algorithm: ImageFeatureAlgorithm = FeaturizationConfig.defaultAlgorithm
) extends LoadableConfig {

	require(codebookSize >= 1 && codebookSize <= 10000, s"${nameOf(codebookSize)} must fall in range [1, 10000]")
	require(featureCount >= 1 && featureCount <= 1000, s"${nameOf(featureCount)} must fall in range [1, 1000]")

}

object FeaturizationConfig {

	val defaultCodebookSize = 500
	val defaultAlgorithm = ImageFeatureAlgorithm.Sift
	val defaultFeatureCount = 10

}

final case class TrainingConfig(
	algorithm: TrainingAlgorithm = TrainingConfig.defaultAlgorithm
) extends LoadableConfig

object TrainingConfig {

	val defaultAlgorithm = TrainingAlgorithm.NearestNeighbor

}

final case class TestingConfig(
	writeSummary: O[String] = None,
	classNames: O[Seq[String]] = None
) {

	require(writeSummary.forall(isValidFilePath), s"${nameOf(writeSummary)} is not a valid file path")

}

final case class Config(
	logLevel: LogLevel = Config.defaultLogLevel,
	sparkLogLevel: LogLevel = Config.defaultSparkLogLevel,
	data: DataConfig,
	featurization: OL[FeaturizationConfig] = None,
	training: OL[TrainingConfig] = None,
	testing: O[TestingConfig] = None
) {

	require(featurization.forall(_.config.isEmpty) || data.hasTrainingSet, s"${nameOf(featurization)} requires a training set")
	require(training.forall(_.config.isEmpty) || featurization.isDefined, s"${nameOf(training)} requires ${nameOf(featurization)}")
	require(testing.isEmpty || data.hasTestSet, s"${nameOf(testing)} requires a test set")
	require(testing.isEmpty || training.isDefined, s"${nameOf(testing)} requires ${nameOf(featurization)}")
	featurization.foreach(_.requireValidPaths)
	training.foreach(_.requireValidPaths)

	def save(file: String) = configToFile(this, file)
	def toJson = configToJson(this)

}

object Config {

	val defaultLogLevel = LogLevel.Info
	val defaultSparkLogLevel = LogLevel.Error

	def fromFile(file: String) = configFromFile(file)
	def fromJson(json: String) = configFromJson(json)

}

