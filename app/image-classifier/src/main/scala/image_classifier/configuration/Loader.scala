package image_classifier.configuration

import com.github.dwickern.macros.NameOf.nameOf
import image_classifier.configuration.LoadMode.LoadMode
import image_classifier.configuration.Utils.{O, isValidFilePath, isValidHdfsFilePath}
import scala.reflect.runtime.universe._

final case class Loader[Type <: LoadableConfig] private[configuration](
	private[configuration] val make: O[Type] = None,
	private[configuration] val load: O[String] = None,
	private[configuration] val save: O[String] = None,
	private[configuration] val loadAndSave: O[String] = None
) {

	{
		import com.github.dwickern.macros.NameOf.nameOf
		val modes = Seq((load, nameOf(load)), (save, nameOf(save)), (loadAndSave, nameOf(loadAndSave))).filter(_._1.isDefined)
		require(modes.length <= 1, s"Cannot define both ${modes(0)} and ${modes(1)}")
		require(load.isDefined || make.isDefined, s"Either ${nameOf(make)} or ${nameOf(load)} must be defined")
		require((save.isEmpty && loadAndSave.isEmpty) || make.isDefined, s"${nameOf(save)} and ${nameOf(loadAndSave)} require ${nameOf(make)}")
	}

	val file: O[String] = load orElse save orElse loadAndSave
	val config = make
	val mode: LoadMode = (make, load, save, loadAndSave) match {
		case (None, Some(load), None, None) => LoadMode.Load
		case (Some(make), Some(load), None, None) => LoadMode.LoadOrMake
		case (Some(make), None, None, None) => LoadMode.Make
		case (Some(make), None, None, Some(loadAndSave)) => LoadMode.LoadOrMakeAndSave
		case (Some(make), None, Some(save), None) => LoadMode.MakeAndSave
		case _ => throw new MatchError((make, load, save, loadAndSave))
	}

	private def requireValidPaths(validator: String => Boolean, errorFormat: String): Unit =
		require(file.forall(validator), errorFormat.format(nameOf(file)))

	private[configuration] def requireValidPaths(implicit tag: TypeTag[Type]) = {
		if (classOf[HdfsLoadableConfig].isAssignableFrom(tag.mirror.runtimeClass(tag.tpe)))
			requireValidHdfsPaths()
		else
			requireValidFsPaths()
	}

	private def requireValidFsPaths(): Unit = requireValidPaths(isValidFilePath, "%s is not a valid file path")
	private def requireValidHdfsPaths(): Unit = requireValidPaths(isValidHdfsFilePath, "%s is not a valid hdfs file path")

	private def hrMode = mode match {
		case LoadMode.Load => s"Load '${file.get}'"
		case LoadMode.LoadOrMake => s"Load '${file.get}' or make"
		case LoadMode.Make => s"Make"
		case LoadMode.MakeAndSave => s"Make and save '${file.get}'"
		case LoadMode.LoadOrMakeAndSave => s"Load or make and save '${file.get}'"
	}

	override def toString: String = s"<$hrMode>"

}

object Loader {

	import LoadMode._

	def loadOrMakeAndSave[Type <: LoadableConfig](file: String, config: Type): Loader[Type] =
		create(LoadOrMakeAndSave, Some(file), Some(config))

	def makeAndSave[Type <: LoadableConfig](config: Type, file: String): Loader[Type] =
		create(MakeAndSave, Some(file), Some(config))

	def load[Type <: LoadableConfig](file: String): Loader[Type] =
		create(Load, Some(file), None)

	def loadOrMake[Type <: LoadableConfig](file: String, config: Type): Loader[Type] =
		create(LoadOrMake, Some(file), Some(config))

	def make[Type <: LoadableConfig](config: Type): Loader[Type] =
		create(Make, None, Some(config))

	def create[Type <: LoadableConfig](mode: LoadMode, file: O[String], config: O[Type]): Loader[Type] = mode match {
		case Make => Loader(config, None, None, None)
		case MakeAndSave => Loader(config, None, file, None)
		case LoadOrMakeAndSave => Loader(config, None, None, file)
		case LoadOrMake => Loader(config, file, None, None)
		case Load => Loader(None, file, None, None)
	}

}