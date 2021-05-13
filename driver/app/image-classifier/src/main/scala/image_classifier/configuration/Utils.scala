package image_classifier.configuration

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

	def configFromJson(json: String): Config = {
		import com.github.plokhotnyuk.jsoniter_scala.core.readFromString
		readFromString[Config](json)
	}

	def isValidFilePath(file: String): Boolean = {
		import java.nio.file.{Paths, InvalidPathException}
		try {
			Paths.get(file)
			true
		} catch {
			case _: InvalidPathException => false
		}
	}

	def isValidHdfsFilePath(file: String): Boolean = {
		import java.net.URI
		try {
			val uri = URI.create(file)
			uri.getScheme == "hdfs" && isValidFilePath(uri.getPath)
		} catch {
			case _: IllegalArgumentException => false
		}
	}

	type P[Type] = Option[Type]
	type L[Type <: LoadableConfig] = Loader[Type]
	type PL[Type <: LoadableConfig] = P[L[Type]]

}

