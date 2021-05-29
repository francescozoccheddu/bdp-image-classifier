package image_classifier.configuration

import java.io.File
import java.nio.charset.Charset
import com.github.plokhotnyuk.jsoniter_scala.core.{JsonValueCodec, readFromString, writeToString}
import com.github.plokhotnyuk.jsoniter_scala.macros.{CodecMakerConfig, JsonCodecMaker}
import image_classifier.utils.FileUtils
import org.apache.commons.io.{FileUtils => LocalFileUtils}

private[configuration] object Utils {

	private implicit val codec: JsonValueCodec[Config] = JsonCodecMaker.make(CodecMakerConfig.withSkipUnexpectedFields(false))
	type O[Type] = Option[Type]
	type L[Type <: LoadableConfig] = Loader[Type]
	type OL[Type <: LoadableConfig] = O[L[Type]]

	def configToFile(config: Config, file: String): Unit =
		LocalFileUtils.writeStringToFile(new File(file), configToJson(config), Charset.defaultCharset())

	def configToJson(config: Config): String =
		writeToString(config)

	def configToFile(config: Config, file: String, fileUtils: FileUtils): Unit =
		fileUtils.writeString(file, configToJson(config))

	def configFromFile(file: String): Config =
		configFromJson(LocalFileUtils.readFileToString(new File(file), Charset.defaultCharset()))

	def configFromFile(file: String, fileUtils: FileUtils): Config =
		configFromJson(fileUtils.readString(file))

	def configFromJson(json: String): Config =
		readFromString[Config](json)

}

