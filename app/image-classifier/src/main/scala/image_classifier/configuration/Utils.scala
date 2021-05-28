package image_classifier.configuration

import image_classifier.utils.FileUtils

private[configuration] object Utils {

	import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
	import com.github.plokhotnyuk.jsoniter_scala.macros.{CodecMakerConfig, JsonCodecMaker}

	private implicit val codec: JsonValueCodec[Config] = JsonCodecMaker.make(CodecMakerConfig.withSkipUnexpectedFields(false))

	def configToFile(config: Config, file: String): Unit = {
		import org.apache.commons.io.FileUtils
		import java.io.File
		import java.nio.charset.Charset
		FileUtils.writeStringToFile(new File(file), configToJson(config), Charset.defaultCharset())
	}

	def configToFile(config: Config, file : String, fileUtils: FileUtils): Unit =
		fileUtils.writeString(file, configToJson(config))

	def configToJson(config: Config): String = {
		import com.github.plokhotnyuk.jsoniter_scala.core.writeToString
		writeToString(config)
	}

	def configFromFile(file: String): Config = {
		import org.apache.commons.io.FileUtils
		import java.io.File
		import java.nio.charset.Charset
		configFromJson(FileUtils.readFileToString(new File(file), Charset.defaultCharset()))
	}

	def configFromFile(file : String, fileUtils: FileUtils) =
		configFromJson(fileUtils.readString(file))

	def configFromJson(json: String): Config = {
		import com.github.plokhotnyuk.jsoniter_scala.core.readFromString
		readFromString[Config](json)
	}

	type O[Type] = Option[Type]
	type L[Type <: LoadableConfig] = Loader[Type]
	type OL[Type <: LoadableConfig] = O[L[Type]]

}

