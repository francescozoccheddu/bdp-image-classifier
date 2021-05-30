package image_classifier.configuration

import scala.util.Random
import com.github.dwickern.macros.NameOf._
import image_classifier.configuration
import image_classifier.configuration.ImageFeatureAlgorithm.ImageFeatureAlgorithm
import image_classifier.configuration.TrainingAlgorithm.TrainingAlgorithm
import image_classifier.configuration.Utils._
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

	requireIn(nameOf(testFraction), testFraction, 0, 1)
	requireFile(nameOf(tempFile), tempFile)

	def labelsCount: Int = dataSet.map(_.length).getOrElse(0) max trainingSet.map(_.length).getOrElse(0) max testSet.map(_.length).getOrElse(0)

}

object DataConfig {

	val defaultTempFile: String = "hdfs://localhost:9000/image-classifier/temp_data"

}

object JointDataConfig {

	val defaultStratified: Boolean = true
	val defaultTestFraction: Double = 0.2

	def defaultSplitSeed: Int = Random.nextInt

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
	val defaultFeatureCount: Int = 50
	val defaultHessianThreshold: Double = 100
	val defaultMaxSize: Int = 1024

}

object CodebookConfig {

	val defaultStratifiedSampling: Boolean = true
	val defaultMaxIterations: Int = 10
	val defaultConvergenceTolerance: Double = 0.0001
	val defaultSize: Int = 500
	val defaultAssignNearest: Boolean = false
	val defaultInitSteps: Int = 2
	val defaultSampleFraction: Double = 1

	def defaultSampleSeed: Int = Random.nextInt

	def defaultSeed: Int = Random.nextInt

}

final case class CodebookConfig(
                                 size: Int = CodebookConfig.defaultSize,
                                 assignNearest: Boolean = CodebookConfig.defaultAssignNearest,
                                 maxIterations: Int = CodebookConfig.defaultMaxIterations,
                                 convergenceTolerance: Double = CodebookConfig.defaultConvergenceTolerance,
                                 seed: Int = CodebookConfig.defaultSeed,
                                 initSteps: Int = CodebookConfig.defaultInitSteps,
                                 sampleFraction: Double = CodebookConfig.defaultSampleFraction,
                                 sampleSeed: Int = CodebookConfig.defaultSampleSeed,
                                 labelsCount: O[Int] = None,
                                 stratifiedSampling: Boolean = CodebookConfig.defaultStratifiedSampling
                               ) {

	requireIn(nameOf(size), size, 10, 100000)
	requireIn(nameOf(maxIterations), maxIterations, 1, 100)
	requireIn(nameOf(initSteps), initSteps, 1, 10)
	requireIn(nameOf(convergenceTolerance), convergenceTolerance, 0, 0.1, false)
	requireIn(nameOf(sampleFraction), sampleFraction, 0, 1, false)
	labelsCount.foreach(requirePositive(nameOf(labelsCount), _))

}

final case class DescriptorConfig(
                                   algorithm: ImageFeatureAlgorithm = DescriptorConfig.defaultAlgorithm,
                                   featureCount: Int = DescriptorConfig.defaultFeatureCount,
                                   octaveLayerCount: Int = DescriptorConfig.defaultOctaveLayerCount,
                                   contrastThreshold: Double = DescriptorConfig.defaultContrastThreshold,
                                   edgeThreshold: Double = DescriptorConfig.defaultEdgeThreshold,
                                   sigma: Double = DescriptorConfig.defaultSigma,
                                   hessianThreshold: Double = DescriptorConfig.defaultHessianThreshold,
                                   octaveCount: Int = DescriptorConfig.defaultOctaveCount,
                                   maxSize: Int = DescriptorConfig.defaultMaxSize
                                 ) {

	requireIn(nameOf(featureCount), featureCount, 1, 100000)
	requireIn(nameOf(octaveLayerCount), octaveLayerCount, 1, 16)
	requireIn(nameOf(contrastThreshold), contrastThreshold, 0, 1000)
	requireIn(nameOf(edgeThreshold), edgeThreshold, 0, 1000)
	requireIn(nameOf(maxSize), maxSize, 64, 8192)
	requireIn(nameOf(sigma), sigma, 0, 16, false)

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
                                 labelsCount: O[Int] = None,
                                 seed: Int = TrainingConfig.defaultSeed
                               ) extends LoadableConfig {

	requireIn(nameOf(maxIterations), maxIterations, 1, 10000)
	requireIn(nameOf(blockSize), blockSize, 4, 8192)
	requireIn(nameOf(elasticNetParam), elasticNetParam, 0, 1)
	requirePositive(nameOf(stepSize), stepSize)
	requireNonNegative(nameOf(regParam), regParam)
	requireNonNegative(nameOf(treeCount), treeCount)
	labelsCount.foreach(requirePositive(nameOf(labelsCount), _))
	hiddenLayers.foreach(requirePositive(nameOf(hiddenLayers), _))

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

	def defaultSeed: Int = Random.nextInt

}

final case class TestingConfig(
                                save: O[String] = None,
                                labels: O[Seq[String]] = None,
                                print: Boolean = TestingConfig.defaultPrint
                              ) {

	save.foreach(requireFile(nameOf(save), _))

}

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


