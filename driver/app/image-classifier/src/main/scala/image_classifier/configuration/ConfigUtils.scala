package image_classifier.configuration

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonValueCodec, JsonReader, JsonWriter}
import com.github.plokhotnyuk.jsoniter_scala.macros.{JsonCodecMaker, CodecMakerConfig}

private[configuration] object ConfigUtils {
	
	private implicit val loadCodec: JsonValueCodec[Load] = new JsonValueCodec[Load] {
		
		override def decodeValue(reader: JsonReader, default: Load): Load = Load(reader.readString(null))
		
		override def encodeValue(value: Load, writer: JsonWriter): Unit = writer.writeVal(value.file)
		
		override val nullValue: Load = null
		
	}

	private implicit val loadableCodec: JsonValueCodec[] = JsonCodecMaker.make
	
	private implicit val codec: JsonValueCodec[Config] = JsonCodecMaker.make(CodecMakerConfig.withSkipUnexpectedFields(false))
	
	def toFile(config : Config, file : String): Unit = {
		import java.io.File
		import org.apache.commons.io.FileUtils
		FileUtils.writeStringToFile(new File(file), toJson(config))
	}
	
	def toJson(config : Config): String = {
		import com.github.plokhotnyuk.jsoniter_scala.core.writeToString
		writeToString(config)
	}

	def fromFile(file : String) : Config = {
		import java.io.File
		import org.apache.commons.io.FileUtils
		fromJson(FileUtils.readFileToString(new File(file)))
	}

	def fromJson(json : String) : Config = {
		import com.github.plokhotnyuk.jsoniter_scala.core.readFromString
		readFromString[Config](json)
	}

}