package image_classifier.configuration

import scala.util.Random
import com.github.dwickern.macros.NameOf._
import image_classifier.configuration
import image_classifier.configuration.ImageFeatureAlgorithm.ImageFeatureAlgorithm
import image_classifier.configuration.TrainingAlgorithm.TrainingAlgorithm
import image_classifier.configuration.Utils._
import image_classifier.utils.FileUtils
import image_classifier.utils.OptionImplicits.OptionExtension
import org.apache.log4j.Logger

private[image_classifier] sealed trait LoadableConfig

final case class DataConfig private[configuration](
                                                    tempFile: String = DataConfig.defaultTempFile,
                                                    dataSet: O[Seq[Seq[String]]] = None,
                                                    trainingSet: O[Seq[Seq[String]]] = None,
                                                    testSet: O[Seq[Seq[String]]] = None,
                                                    testSample: SampleConfig = DataConfig.defaultTestSample
                                                  ) extends LoadableConfig {

	def labelsCount: Int = dataSet.map(_.length).getOrElse(0) max trainingSet.map(_.length).getOrElse(0) max testSet.map(_.length).getOrElse(0)

}

object DataConfig {

	val defaultTempFile: String = "hdfs:///image-classifier/temp_data"
	val defaultTestSample: SampleConfig = SampleConfig(maxFractionPerClass = 0.2)

}

object JointDataConfig {

	val defaultStratified: Boolean = true
	val defaultTestFraction: Double = 0.2
	val defaultSplitSeed: Option[Int] = Some(0)

}

final case class FeaturizationConfig(
                                      codebook: CodebookConfig = CodebookConfig(),
                                      descriptor: DescriptorConfig = DescriptorConfig()
                                    ) extends LoadableConfig

object DescriptorConfig {

	val defaultAlgorithm: configuration.ImageFeatureAlgorithm.Value = ImageFeatureAlgorithm.Sift
	val defaultOctaveLayerCount: Int = 3
	val defaultOctaveCount: Int = 4
	val defaultContrastThreshold: Double = 0.04
	val defaultEdgeThreshold: Int = 30
	val defaultSigma: Double = 1.6
	val defaultMaxFeatureCount: Int = 50
	val defaultHessianThreshold: Double = 100
	val defaultMaxSize: Int = 1024
	val defaultUseImageIO: Boolean = false
	val defaultMinFeatureCount: Int = 1

}

object CodebookConfig {

	val defaultMaxIterations: Int = 10
	val defaultConvergenceTolerance: Double = 0.0001
	val defaultSize: Int = 500
	val defaultAssignNearest: Boolean = false
	val defaultInitSteps: Int = 2
	val defaultImageSample: SampleConfig = SampleConfig()
	val defaultFeatureSample: SampleConfig = SampleConfig()
	val defaultSeed: Option[Int] = Some(0)

}

object SampleConfig {

	val defaultMaxCount: Int = Int.MaxValue
	val defaultMaxCountPerClass: Int = Int.MaxValue
	val defaultMaxFraction: Double = 1
	val defaultMaxFractionPerClass: Double = 1
	val defaultMaxMaxMinFractionPerClass: Double = Double.PositiveInfinity
	val defaultSeed: Option[Int] = Some(0)

}

final case class SampleConfig(
                               maxCount: Int = SampleConfig.defaultMaxCount,
                               maxCountPerClass: Int = SampleConfig.defaultMaxCountPerClass,
                               maxFraction: Double = SampleConfig.defaultMaxFraction,
                               maxFractionPerClass: Double = SampleConfig.defaultMaxFractionPerClass,
                               maxMaxMinFractionPerClass: Double = SampleConfig.defaultMaxMaxMinFractionPerClass,
                               seed: Option[Int] = SampleConfig.defaultSeed
                             ) {

	requirePositive(nameOf(maxCount), maxCount)
	requirePositive(nameOf(maxCountPerClass), maxCountPerClass)
	requireIn(nameOf(maxFraction), maxFraction, 0, 1, false)
	requireIn(nameOf(maxFractionPerClass), maxFractionPerClass, 0, 1, false)
	requireMin(nameOf(maxMaxMinFractionPerClass), maxMaxMinFractionPerClass, 1)

	lazy val actualSeed: Int = seed.getOr(Random.nextInt)

	def canSkip: Boolean =
		maxCount == Int.MaxValue &&
		  maxCountPerClass == Int.MaxValue &&
		  maxFraction == 1 && maxFractionPerClass == 1 &&
		  maxMaxMinFractionPerClass == Double.PositiveInfinity

}

final case class CodebookConfig(
                                 size: Int = CodebookConfig.defaultSize,
                                 assignNearest: Boolean = CodebookConfig.defaultAssignNearest,
                                 maxIterations: Int = CodebookConfig.defaultMaxIterations,
                                 convergenceTolerance: Double = CodebookConfig.defaultConvergenceTolerance,
                                 seed: Option[Int] = CodebookConfig.defaultSeed,
                                 initSteps: Int = CodebookConfig.defaultInitSteps,
                                 imageSample: SampleConfig = CodebookConfig.defaultImageSample,
                                 featureSample: SampleConfig = CodebookConfig.defaultFeatureSample
                               ) {

	lazy val actualSeed: Int = seed.getOr(Random.nextInt)

	requireIn(nameOf(size), size, 10, 100000)
	requireIn(nameOf(maxIterations), maxIterations, 1, 100)
	requireIn(nameOf(initSteps), initSteps, 1, 10)
	requireIn(nameOf(convergenceTolerance), convergenceTolerance, 0, 0.1, false)

}

final case class DescriptorConfig(
                                   algorithm: ImageFeatureAlgorithm = DescriptorConfig.defaultAlgorithm,
                                   maxFeatureCount: Int = DescriptorConfig.defaultMaxFeatureCount,
                                   octaveLayerCount: Int = DescriptorConfig.defaultOctaveLayerCount,
                                   contrastThreshold: Double = DescriptorConfig.defaultContrastThreshold,
                                   edgeThreshold: Double = DescriptorConfig.defaultEdgeThreshold,
                                   sigma: Double = DescriptorConfig.defaultSigma,
                                   hessianThreshold: Double = DescriptorConfig.defaultHessianThreshold,
                                   octaveCount: Int = DescriptorConfig.defaultOctaveCount,
                                   maxSize: Int = DescriptorConfig.defaultMaxSize,
                                   minFeatureCount: Int = DescriptorConfig.defaultMinFeatureCount,
                                   useImageIO: Boolean = DescriptorConfig.defaultUseImageIO
                                 ) {

	requireIn(nameOf(maxFeatureCount), maxFeatureCount, 1, 100000)
	requireIn(nameOf(octaveLayerCount), octaveLayerCount, 1, 16)
	requireIn(nameOf(contrastThreshold), contrastThreshold, 0, 1000)
	requireIn(nameOf(edgeThreshold), edgeThreshold, 0, 1000)
	requireIn(nameOf(maxSize), maxSize, 64, 8192)
	requireIn(nameOf(sigma), sigma, 0, 16, false)
	requireIn(nameOf(minFeatureCount), minFeatureCount, 1, maxFeatureCount)

}

final case class TrainingConfig(
                                 algorithm: TrainingAlgorithm = TrainingConfig.defaultAlgorithm,
                                 maxIterations: Int = TrainingConfig.defaultMaxIterations,
                                 regParam: Double = TrainingConfig.defaultRegParam,
                                 elasticNetParam: Double = TrainingConfig.defaultElasticNetParam,
                                 treeCount: Int = TrainingConfig.defaultTreeCount,
                                 hiddenLayers: Seq[Int] = TrainingConfig.defaultHiddenLayers,
                                 stepSize: Double = TrainingConfig.defaultStepSize,
                                 convergenceTolerance: Double = TrainingConfig.defaultConvergenceTolerance,
                                 depth: Int = TrainingConfig.defaultDepth,
                                 maxBins: Int = TrainingConfig.defaultMaxBins,
                                 minInfoGain: Double = TrainingConfig.defaultMinInfoGain,
                                 factorSize: Int = TrainingConfig.defaultFactorSize,
                                 seed: Option[Int] = TrainingConfig.defaultSeed
                               ) extends LoadableConfig {

	lazy val actualSeed: Int = seed.getOr(Random.nextInt)

	requireIn(nameOf(maxIterations), maxIterations, 1, 10000)
	requireNonNegative(nameOf(regParam), regParam)
	requireIn(nameOf(elasticNetParam), elasticNetParam, 0, 1)
	requireIn(nameOf(treeCount), treeCount, 1, 10000)
	hiddenLayers.foreach(requirePositive(nameOf(hiddenLayers), _))
	requirePositive(nameOf(stepSize), stepSize)
	requireNonNegative(nameOf(convergenceTolerance), convergenceTolerance)
	requireIn(nameOf(depth), depth, 2, 10000)
	requireIn(nameOf(maxBins), maxBins, 2, 10000)
	requireNonNegative(nameOf(minInfoGain), minInfoGain)
	requirePositive(nameOf(factorSize), factorSize)

}

object TrainingConfig {

	val defaultAlgorithm: configuration.TrainingAlgorithm.Value = TrainingAlgorithm.NaiveBayes
	val defaultMaxIterations: Int = 100
	val defaultRegParam: Double = 0
	val defaultElasticNetParam: Double = 0
	val defaultTreeCount: Int = 20
	val defaultHiddenLayers: Seq[Int] = Seq.empty
	val defaultStepSize: Double = 0.03
	val defaultConvergenceTolerance: Double = 1e-6
	val defaultMaxBins: Int = 32
	val defaultDepth: Int = 5
	val defaultMinInfoGain: Double = 0
	val defaultFactorSize: Int = 8
	val defaultSeed: Option[Int] = Some(0)

}

final case class TestingConfig(
                                save: O[String] = None,
                                labels: O[Seq[String]] = None,
                                print: Boolean = TestingConfig.defaultPrint
                              )

object TestingConfig {

	val defaultPrint: Boolean = true

}

final case class Config(
                         data: OL[DataConfig],
                         featurization: OL[FeaturizationConfig] = None,
                         training: OL[TrainingConfig] = None,
                         testing: O[TestingConfig] = None
                       ) {

	requireDep(nameOf(featurization), featurization, nameOf(data), data)
	requireDep(nameOf(training), training, nameOf(featurization), featurization)
	requireDep(nameOf(testing), testing, nameOf(training), training)
	requireDep(nameOf(testing), testing, nameOf(featurization), featurization)

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


