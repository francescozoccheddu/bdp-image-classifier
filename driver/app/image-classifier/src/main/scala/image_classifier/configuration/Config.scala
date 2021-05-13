package image_classifier.configuration

import com.github.dwickern.macros.NameOf._
import image_classifier.configuration.ImageFeatureAlgorithm.ImageFeatureAlgorithm
import image_classifier.configuration.LogLevel.LogLevel
import image_classifier.configuration.TrainingAlgorithm.TrainingAlgorithm
import image_classifier.configuration.Utils._
import scala.reflect.runtime.universe._

final case class Loader[Type <: LoadableConfig](
	load: Option[String],
	save: Option[String],
	make: Option[Type]
) /*(implicit tag: TypeTag[Type])*/ {

	require(save.isEmpty || make.isDefined, s"${nameOf(save)} requires ${nameOf(make)}")
	require(load.isDefined || make.isDefined, s"Either ${nameOf(load)} or ${nameOf(make)} must be defined")

	/*{
		val (validator, errorFormat): (String => Boolean, String) =
			if (classOf[HdfsLoadableConfig].isAssignableFrom(tag.mirror.runtimeClass(tag.tpe)))
				(isValidHdfsFilePath: String => Boolean, "%s is not a valid hdfs file path")
			else
				(isValidFilePath: String => Boolean, "%s is not a valid file path")
		require(save.forall(validator), errorFormat.format(nameOf(save)))
		require(load.forall(validator), errorFormat.format(nameOf(load)))
	}*/

}

object Loader {

	def loadOrMakeAndSave[Type <: LoadableConfig](file: String, config: Type): Loader[Type] =
		Loader(Some(file), Some(file), Some(config))

	def makeAndSave[Type <: LoadableConfig](file: String, config: Type): Loader[Type] =
		Loader(None, Some(file), Some(config))

	def load[Type <: LoadableConfig](file: String): Loader[Type] =
		Loader(Some(file), Some(file), None)

	def loadOrMake[Type <: LoadableConfig](file: String, config: Type): Loader[Type] =
		Loader(Some(file), Some(file), Some(config))

	def make[Type <: LoadableConfig](config: Type): Loader[Type] =
		Loader(None, None, Some(config))

}

sealed trait LoadableConfig

sealed trait HdfsLoadableConfig extends LoadableConfig

sealed trait DataConfig extends LoadableConfig {

	def hasTestSet: Boolean
	def hasTrainingSet: Boolean

}

sealed trait DataSetConfig {

	def tempFile: P[String]

	require(tempFile.forall(isValidHdfsFilePath), s"${nameOf(tempFile)} is not a valid hdfs file path")

}

final case class SplitDataSetConfig(
	override val tempFile: P[String] = None,
	classFiles: Seq[Seq[String]]
) extends HdfsLoadableConfig with DataSetConfig

final case class SplitDataConfig(
	test: PL[SplitDataSetConfig],
	training: PL[SplitDataSetConfig]
) extends DataConfig {

	override def hasTestSet: Boolean = test.isDefined
	override def hasTrainingSet: Boolean = training.isDefined

}

final case class JointDataConfig(
	override val tempFile: P[String] = None,
	classFiles: Seq[Seq[String]],
	testFraction: Double = JointDataConfig.defaultTestFraction,
	splitSeed: Int = JointDataConfig.defaultSplitSeed,
	stratified: Boolean = JointDataConfig.defaultStratified
) extends HdfsLoadableConfig with DataSetConfig with DataConfig {

	override def hasTestSet: Boolean = testFraction > 0
	override def hasTrainingSet: Boolean = testFraction < 1

	require(testFraction >= 0 && testFraction <= 1, s"${nameOf(testFraction)} must fall in range [0, 1]")

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
	val defaultAlgorithm = ImageFeatureAlgorithm.SIFT
	val defaultFeatureCount = 10

}

final case class TrainingConfig(
	algorithm: TrainingAlgorithm = TrainingConfig.defaultAlgorithm
) extends LoadableConfig

object TrainingConfig {

	val defaultAlgorithm = TrainingAlgorithm.NN

}

final case class TestingConfig(
	writeSummary: P[String] = None,
	classNames: P[Seq[String]] = None
) {

	require(writeSummary.forall(isValidFilePath), s"${nameOf(writeSummary)} is not a valid file path")

}

final case class Config(
	logLevel: LogLevel = Config.defaultLogLevel,
	sparkLogLevel: LogLevel = Config.defaultSparkLogLevel,
	data: L[DataConfig],
	featurization: PL[FeaturizationConfig] = None,
	training: PL[TrainingConfig] = None,
	testing: P[TestingConfig] = None
) {

	require(featurization.forall(_.make.isEmpty) || data.make.forall(_.hasTrainingSet), s"${nameOf(featurization)} requires a training set")
	require(training.forall(_.make.isEmpty) || featurization.isDefined, s"${nameOf(training)} requires ${nameOf(featurization)}")
	require(testing.isEmpty || data.make.forall(_.hasTestSet), s"${nameOf(testing)} requires a test set")
	require(testing.isEmpty || training.isDefined, s"${nameOf(testing)} requires ${nameOf(featurization)}")

	def save(file: String) = configToFile(this, file)
	def toJson = configToJson(this)

}

object Config {

	val defaultLogLevel = LogLevel.INFO
	val defaultSparkLogLevel = LogLevel.ERROR

	def fromFile(file: String) = configFromFile(file)
	def fromJson(json: String) = configFromJson(json)

}

