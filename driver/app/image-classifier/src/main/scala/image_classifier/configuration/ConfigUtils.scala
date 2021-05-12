package image_classifier.configuration

private[configuration] object ConfigUtils {

	import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
	import com.github.plokhotnyuk.jsoniter_scala.macros.{CodecMakerConfig, JsonCodecMaker}

	private def makeCodec[LoadableType >: Null <: Loadable[LoadableType]]: JsonValueCodec[LoadableOrLoad[LoadableType]] = new JsonValueCodec[LoadableOrLoad[LoadableType]] {

		import com.github.plokhotnyuk.jsoniter_scala.core.{JsonReader, JsonWriter}

		private val defaultCodec: JsonValueCodec[LoadableType] = JsonCodecMaker.make(CodecMakerConfig.withSkipUnexpectedFields(false))

		override def decodeValue(reader: JsonReader, default: LoadableOrLoad[LoadableType]): LoadableOrLoad[LoadableType] = {
			if (reader.isNextToken('"'))
				Load(reader.readString(null))
			else
				defaultCodec.decodeValue(reader, null)
		}

		override def encodeValue(value: LoadableOrLoad[LoadableType], writer: JsonWriter): Unit = value match {
			case Load(file) => writer.writeVal(file)
			case loadable: LoadableType => defaultCodec.encodeValue(loadable, writer)
		}

		override def nullValue: LoadableOrLoad[LoadableType] = null

	}

	private implicit val splitDataSetCodec = makeCodec[SplitDataSetConfig]
	private implicit val jointDataCodec = makeCodec[JointDataConfig]
	private implicit val featurizationCodec = makeCodec[FeaturizationConfig]
	private implicit val trainingCodec = makeCodec[TrainingConfig]

	private implicit val codec: JsonValueCodec[Config] = JsonCodecMaker.make(CodecMakerConfig.withSkipUnexpectedFields(false))

	def toFile(config: Config, file: String): Unit = {
		import org.apache.commons.io.FileUtils
		import java.io.File
		import java.nio.charset.Charset
		FileUtils.writeStringToFile(new File(file), toJson(config), Charset.defaultCharset())
	}

	def toJson(config: Config): String = {
		import com.github.plokhotnyuk.jsoniter_scala.core.writeToString
		writeToString(config)
	}

	def fromFile(file: String): Config = {
		import org.apache.commons.io.FileUtils
		import java.io.File
		import java.nio.charset.Charset
		fromJson(FileUtils.readFileToString(new File(file), Charset.defaultCharset()))
	}

	def fromJson(json: String): Config = {
		import com.github.plokhotnyuk.jsoniter_scala.core.readFromString
		readFromString[Config](json)
	}

}