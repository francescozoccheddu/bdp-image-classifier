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

	def configFromJson(json: String): Config =
		readFromString[Config](json)

	def configFromFile(file: String, fileUtils: FileUtils): Config =
		configFromJson(fileUtils.readString(file))

	def requireIn[T](name: String, value: T, min: T, max: T, minInclusive: Boolean = true, maxInclusive: Boolean = true)(implicit ordered: T => Ordered[T]): Unit =
		require(
			(if (minInclusive) value >= min else value > min) && (if (maxInclusive) value <= max else value < max),
			s"'$name' must fall in range ${if (minInclusive) '[' else '('}$min,$max${if (maxInclusive) ']' else ')'}")

	def requireMin[T](name: String, value: T, min: T, inclusive: Boolean = true)(implicit ordered: T => Ordered[T]): Unit =
		require(if (inclusive) value >= min else value > min, s"'$name' must be greater than ${if (inclusive) "or equal to " else ""} $min")

	def requireMax[T](name: String, value: T, max: T, inclusive: Boolean = true)(implicit ordered: T => Ordered[T]): Unit =
		require(if (inclusive) value <= max else value < max, s"'$name' must be less than ${if (inclusive) "or equal to " else ""} $max")

	def requirePositive[T](name: String, value: T)(implicit ordered: T => Ordered[T], numeric: Numeric[T]): Unit =
		require(value > numeric.zero, s"'$name' must be positive")

	def requireNonNegative[T](name: String, value: T)(implicit ordered: T => Ordered[T], numeric: Numeric[T]): Unit =
		require(value >= numeric.zero, s"'$name' must be positive or zero")

	def requireNonEmpty(name: String, value: TraversableOnce[_]): Unit =
		require(value.nonEmpty, s"'$name' cannot be empty")

	def requireFile(name: String, value: String): Unit =
		require(FileUtils.isValidPath(value), s"'$name' must be a valid file path")

	def requireDep(name: String, value: Option[_], dependencyName: String, dependencyValue: Option[_]): Unit =
		require(value.isEmpty || dependencyValue.isDefined, s"'$name' requires '$dependencyName'")

}

