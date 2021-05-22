package image_classifier.pipeline

import image_classifier.configuration.{LoadableConfig, Loader}
import org.apache.spark.sql.SparkSession

private[pipeline] abstract class Stage[Result, Specs](val name: String, val specs: Option[Specs]) {
	import image_classifier.pipeline.Stage.logger

	protected def run(specs: Specs): Result

	private var ran = false
	private var optResult: Option[Result] = None

	final def apply = {
		if (!ran) {
			ran = true
			if (specs.isDefined) {
				logger.info(s"Running '$name' stage")
				optResult = Some(run(specs.get))
			}
		}
		optResult
	}

	final def result = apply.get
	final def hasResult = apply.isDefined

}

private[pipeline] abstract class LoaderStage[Result, Config <: LoadableConfig](name: String, loader: Option[Loader[Config]]) extends Stage[Result, Loader[Config]](name, loader) {
	import image_classifier.configuration.LoadMode
	import image_classifier.utils.OptionImplicits._
	import image_classifier.pipeline.LoaderStage.logger

	final override protected def run(specs: Loader[Config]): Result = {
		logger.info(s"Running loader $specs")
		specs.mode match {
			case LoadMode.Load => load(specs.file.get)
			case LoadMode.LoadOrMake => loadIfExists(specs.file.get).getOr(() => make(specs.config.get))
			case LoadMode.LoadOrMakeAndSave => loadIfExists(specs.file.get).getOr(() => makeAndSave(specs.config.get, specs.file.get))
			case LoadMode.MakeAndSave => makeAndSave(specs.config.get, specs.file.get)
			case LoadMode.Make => make(specs.config.get)
		}
	}

	private def loadIfExists(file: String): Option[Result] = {
		import image_classifier.pipeline.utils.Files
		if (Files.exists(file))
			Some(load(file))
		else {
			logger.info(s"File '$file' does not exist")
			None
		}
	}

	private def makeAndSave(config: Config, file: String) = {
		val result = make(config)
		save(result, file)
		result
	}

	protected def load(file: String): Result
	protected def make(config: Config): Result
	protected def save(result: Result, file: String): Unit

}

private[pipeline] object Stage {

	import org.apache.log4j.Logger

	private val logger = Logger.getLogger(Stage.getClass)

}

private[pipeline] object LoaderStage {
	import org.apache.log4j.Logger

	private val logger = Logger.getLogger(LoaderStage.getClass)

}