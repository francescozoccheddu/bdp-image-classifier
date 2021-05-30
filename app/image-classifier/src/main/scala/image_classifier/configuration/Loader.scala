package image_classifier.configuration

import com.github.dwickern.macros.NameOf.nameOf
import image_classifier.configuration.LoadMode.{LoadMode, _}
import image_classifier.configuration.Utils.O
import image_classifier.utils.FileUtils

final case class Loader[Type <: LoadableConfig] private[configuration](
                                                                        private[configuration] val make: O[Type] = None,
                                                                        private[configuration] val load: O[String] = None,
                                                                        private[configuration] val save: O[String] = None,
                                                                        private[configuration] val loadAndSave: O[String] = None
                                                                      ) {

	{
		val modes = Seq((load, nameOf(load)), (save, nameOf(save)), (loadAndSave, nameOf(loadAndSave))).filter(_._1.isDefined)
		require(modes.length <= 1, s"Cannot define both ${modes.head} and ${modes(1)}")
		require(load.isDefined || make.isDefined, s"Either ${nameOf(make)} or ${nameOf(load)} must be defined")
		require((save.isEmpty && loadAndSave.isEmpty) || make.isDefined, s"${nameOf(save)} and ${nameOf(loadAndSave)} require ${nameOf(make)}")
	}

	val file: O[String] = load orElse save orElse loadAndSave
	val config: O[Type] = make
	val mode: LoadMode = (make, load, save, loadAndSave) match {
		case (None, Some(_), None, None) => LoadMode.Load
		case (Some(_), Some(_), None, None) => LoadMode.LoadOrMake
		case (Some(_), None, None, None) => LoadMode.Make
		case (Some(_), None, None, Some(_)) => LoadMode.LoadOrMakeAndSave
		case (Some(_), None, Some(_), None) => LoadMode.MakeAndSave
		case _ => throw new MatchError((make, load, save, loadAndSave))
	}

	require(file.forall(FileUtils.isValidPath), "Not a valid path")

	override def toString: String = s"<$hrMode>"

	private def hrMode: String = mode match {
		case LoadMode.Load => s"Load '${file.get}'"
		case LoadMode.LoadOrMake => s"Load '${file.get}' or make"
		case LoadMode.Make => s"Make"
		case LoadMode.MakeAndSave => s"Make and save '${file.get}'"
		case LoadMode.LoadOrMakeAndSave => s"Load or make and save '${file.get}'"
	}

}

object Loader {

	def loadOrMakeAndSave[Type <: LoadableConfig](file: String, config: Type): Loader[Type] =
		create(LoadOrMakeAndSave, Some(file), Some(config))

	def create[Type <: LoadableConfig](mode: LoadMode, file: O[String], config: O[Type]): Loader[Type] = mode match {
		case Make => Loader(config, None, None, None)
		case MakeAndSave => Loader(config, None, file, None)
		case LoadOrMakeAndSave => Loader(config, None, None, file)
		case LoadOrMake => Loader(config, file, None, None)
		case Load => Loader(None, file, None, None)
	}

	def makeAndSave[Type <: LoadableConfig](config: Type, file: String): Loader[Type] =
		create(MakeAndSave, Some(file), Some(config))

	def load[Type <: LoadableConfig](file: String): Loader[Type] =
		create(Load, Some(file), None)

	def loadOrMake[Type <: LoadableConfig](file: String, config: Type): Loader[Type] =
		create(LoadOrMake, Some(file), Some(config))

	def make[Type <: LoadableConfig](config: Type): Loader[Type] =
		create(Make, None, Some(config))

}