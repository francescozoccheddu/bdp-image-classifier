package image_classifier.pipeline

import image_classifier.configuration.LoadMode.LoadMode
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

	private[pipeline] val file: String = specs.map(_.file.map(fileUtils.resolve).orNull).orNull
	private[pipeline] val loadMode: LoadMode = specs.map(_.mode).orNull
	private[pipeline] val config: Config =
		if (specs.isDefined && specs.get.config.isDefined)
			specs.get.config.get
		else
			null.asInstanceOf[Config]
	private var loaded: Boolean = false

	final override protected def run(specs: Loader[Config]): Result = {
		logger.info(s"Stage '$name': Running loader $specs")
		specs.mode match {
			case LoadMode.Load => loadImpl()
			case LoadMode.LoadOrMake => loadIfExistsImpl().getOr(() => makeImpl())
			case LoadMode.LoadOrMakeAndSave => loadIfExistsImpl().getOr(() => makeAndSaveImpl())
			case LoadMode.MakeAndSave => makeAndSaveImpl()
			case LoadMode.Make => makeImpl()
		}
	}

	def wasLoaded: Boolean = loaded

	def wasMade: Boolean = hasResult && !loaded

	protected def makeImpl(): Result = {
		logger.info(s"Stage '$name': Making")
		make()
	}

	protected def loadImpl(): Result = {
		logger.info(s"Stage '$name': Loading '$file'")
		loaded = true
		load()
	}

	protected def exists(file: String): Boolean = fileUtils.exists(file)

	protected def saveImpl(result: Result): Unit = {
		logger.info(s"Stage '$name': Saving to '$file'")
		fileUtils.makeDirs(FileUtils.parent(file))
		save(result)
	}

	protected def load(): Result

	protected def make(): Result

	protected def save(result: Result): Unit

	private def loadIfExistsImpl(): Option[Result] =
		if (exists(file))
			Some(loadImpl())
		else {
			logger.info(s"Stage '$name': File '$file' does not exist")
			None
		}

	private def makeAndSaveImpl(): Result = {
		val result = make()
		logger.info(s"Stage '$name': Saving to '$file'")
		save(result)
		result
	}

}

private[pipeline] object Stage {

	private val logger: Logger = Logger.getLogger(getClass)

}

private[pipeline] object LoaderStage {

	private val logger: Logger = Logger.getLogger(getClass)

}