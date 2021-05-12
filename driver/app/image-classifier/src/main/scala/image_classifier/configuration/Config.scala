package image_classifier.configuration

object ImageFeatureAlgorithm extends Enumeration {

	type ImageFeatureAlgorithm = Value
	val SIFT = Value("sift")

}

object LogLevel extends Enumeration {

	type LogLevel = Value
	val INFO = Value("info")
	val WARN = Value("warn")
	val ERROR = Value("error")
	val OFF = Value("off")

}

object TrainingAlgorithm extends Enumeration {

	type TrainingAlgorithm = Value
	val NN = Value("nn")

}

import image_classifier.configuration.ImageFeatureAlgorithm.ImageFeatureAlgorithm
import image_classifier.configuration.LogLevel.LogLevel
import image_classifier.configuration.TrainingAlgorithm.TrainingAlgorithm

private[configuration] object Types {

	type L[LoadableType <: Loadable[LoadableType]] = LoadableOrLoad[LoadableType]
	type P[Type] = Option[Type]
	type PL[LoadableType <: Loadable[LoadableType]] = P[L[LoadableType]]

}

import image_classifier.configuration.Types._

sealed abstract class LoadableOrLoad[+LoadableType <: Loadable[LoadableType]]

final case class Load(file: String) extends LoadableOrLoad[Nothing]

sealed abstract class Loadable[+LoadableType <: Loadable[LoadableType]] extends LoadableOrLoad[LoadableType] {

	def saveFile: Option[String]

}

sealed trait DataConfig

final case class SplitDataSetConfig(
	override val saveFile: P[String],
	classFiles: Seq[Seq[String]]
) extends Loadable[SplitDataSetConfig]

final case class SplitDataConfig(
	test: PL[SplitDataSetConfig],
	training: PL[SplitDataSetConfig]
) extends DataConfig

final case class JointDataConfig(
	override val saveFile: P[String],
	classFiles: Seq[Seq[String]],
	testFraction: Double = JointDataConfig.defaultTestFraction,
	splitSeed: Int = JointDataConfig.defaultSplitSeed,
	stratified: Boolean = JointDataConfig.defaultStratified
) extends Loadable[JointDataConfig] with DataConfig

object JointDataConfig {

	val defaultTestFraction = 0.2
	val defaultStratified = true
	def defaultSplitSeed = util.Random.nextInt

}

final case class FeaturizationConfig(
	override val saveFile: P[String],
	codebookSize: Int = FeaturizationConfig.defaultCodebookSize,
	featureCount: Int = FeaturizationConfig.defaultFeatureCount,
	algorithm: ImageFeatureAlgorithm = FeaturizationConfig.defaultAlgorithm
) extends Loadable[FeaturizationConfig]

object FeaturizationConfig {

	val defaultCodebookSize = 500
	val defaultAlgorithm = ImageFeatureAlgorithm.SIFT
	val defaultFeatureCount = 10

}

final case class TrainingConfig(
	override val saveFile: P[String],
	algorithm: TrainingAlgorithm = TrainingConfig.defaultAlgorithm
) extends Loadable

object TrainingConfig {

	val defaultAlgorithm: TrainingAlgorithm = TrainingAlgorithm.NN

}

final case class TestingConfig(
	writeSummary: P[String],
	classNames: P[Seq[String]]
)

final case class Config(
	logLevel: LogLevel = Config.defaultLogLevel,
	sparkLogLevel: LogLevel = Config.defaultSparkLogLevel,
	data: DataConfig,
	featurization: P[FeaturizationConfig],
	training: P[TrainingConfig],
	testing: P[TestingConfig]
)

object Config {

	val defaultLogLevel = LogLevel.INFO
	val defaultSparkLogLevel = LogLevel.ERROR

	def load(file: String): Config = {
		???
	}

	def save(file: String): Unit = {
		???
	}

}

