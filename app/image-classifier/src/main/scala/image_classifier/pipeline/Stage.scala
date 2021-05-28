package image_classifier.pipeline

import image_classifier.configuration.{LoadMode, LoadableConfig, Loader}
import image_classifier.pipeline.LoaderStage.logger
import image_classifier.pipeline.Stage.logger
import image_classifier.utils.FileUtils
import image_classifier.utils.OptionImplicits._
import org.apache.log4j.Logger

private[pipeline] abstract class Stage[Result, Specs](val name: String, val specs: Option[Specs])(implicit protected val fileUtils: FileUtils) {

	private var ran: Boolean = false
	private var optResult: Option[Result] = None

	final def result: Result = apply.get

	final def apply: Option[Result] = {
		if (!ran) {
			ran = true
			if (specs.isDefined) {
				logger.info(s"Running '$name' stage")
				optResult = Some(run(specs.get))
			}
		}
		optResult
	}

	final def hasResult: Boolean = apply.isDefined

	protected def run(specs: Specs): Result

}

private[pipeline] abstract class LoaderStage[Result, Config <: LoadableConfig](name: String, loader: Option[Loader[Config]])(implicit fileUtils: FileUtils)
  extends Stage[Result, Loader[Config]](name, loader)(fileUtils) {

	final override protected def run(specs: Loader[Config]): Result = {
		logger.info(s"Running loader $specs")
		specs.mode match {
			case LoadMode.Load => loadImpl(specs.file.get)
			case LoadMode.LoadOrMake => loadIfExistsImpl(specs.file.get).getOr(() => makeImpl(specs.config.get))
			case LoadMode.LoadOrMakeAndSave => loadIfExistsImpl(specs.file.get).getOr(() => makeAndSaveImpl(specs.config.get, specs.file.get))
			case LoadMode.MakeAndSave => makeAndSaveImpl(specs.config.get, specs.file.get)
			case LoadMode.Make => makeImpl(specs.config.get)
		}
	}

	protected def makeImpl(config: Config): Result = {
		logger.info(s"Making")
		make(config)
	}

	private def loadIfExistsImpl(file: String): Option[Result] =
		if (exists(file))
			Some(loadImpl(file))
		else {
			logger.info(s"File '$file' does not exist")
			None
		}

	protected def loadImpl(file: String): Result = {
		logger.info(s"Loading '$file'")
		load(file)
	}

	protected def exists(file: String): Boolean = fileUtils.exists(file)

	private def makeAndSaveImpl(config: Config, file: String): Result = {
		val result = make(config)
		logger.info(s"Saving to '$file'")
		save(result, file)
		result
	}

	protected def saveImpl(result: Result, file: String): Unit = {
		logger.info(s"Saving to '$file'")
		fileUtils.makeDirs(FileUtils.parent(file))
		save(result, file)
	}

	protected def load(file: String): Result

	protected def make(config: Config): Result

	protected def save(result: Result, file: String): Unit

}

private[pipeline] object Stage {

	private val logger: Logger = Logger.getLogger(getClass)

}

private[pipeline] object LoaderStage {

	private val logger: Logger = Logger.getLogger(getClass)

}