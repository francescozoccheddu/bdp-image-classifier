package image_classifier.configuration

import com.github.dwickern.macros.NameOf._
import image_classifier.configuration.ImageFeatureAlgorithm.ImageFeatureAlgorithm
import image_classifier.configuration.TrainingAlgorithm.TrainingAlgorithm
import image_classifier.configuration.Utils._

private[configuration] sealed trait LoadableConfig

private[configuration] sealed trait HdfsLoadableConfig extends LoadableConfig

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

	private val logger = Logger.getLogger(Config.getClass)

	def fromFile(file: String) = {
		logger.info("Loading config file '$file'")
		configFromFile(file)
	}

	def fromJson(json: String) = {
		logger.info("Writing config file '$file'")
		configFromJson(json)
	}

}

