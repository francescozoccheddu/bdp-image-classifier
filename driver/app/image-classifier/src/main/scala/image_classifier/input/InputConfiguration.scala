package image_classifier.input

import InputOptions.{defaultCodebookSize, defaultLocalFeaturesCount}
import InputConfiguration.defaultTestFraction

private[input] case class InputConfiguration(
		classes: Seq[InputClass],
		codebookSize: Int = defaultCodebookSize,
		localFeaturesCount: Int = defaultLocalFeaturesCount,
		testFraction: Double = defaultTestFraction,
		testSeed: Option[Int] = None
) {

	require(classes.nonEmpty)
	require(codebookSize > 10 && codebookSize < 1000)
	require(localFeaturesCount > 0 && localFeaturesCount < 100)
	require(testFraction >= 0 && testFraction <= 1)

	def toOptions: InputOptions = InputOptions(codebookSize, localFeaturesCount)

}

private[input] object InputConfiguration {

	val defaultTestFraction: Double = 0.2

	def fromFile(configFile: String): InputConfiguration = {
		import scala.io.Source
		val source = Source.fromFile(configFile)
		val json =
			try source.getLines.mkString("\n")
			finally source.close
		fromJson(json)
	}

	private def fromJson(configJsonString: String): InputConfiguration = {
	import com.github.plokhotnyuk.jsoniter_scala.macros._
	import com.github.plokhotnyuk.jsoniter_scala.core._
	implicit val codec: JsonValueCodec[InputConfiguration] = JsonCodecMaker.make
	readFromArray(configJsonString.getBytes("UTF-8"))
	}

}
