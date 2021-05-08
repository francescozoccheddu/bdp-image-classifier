package image_classifier.input

import InputOptions.{defaultCodebookSize, defaultLocalFeaturesCount, defaultLocalFeaturesAlgorithm, defaultMaxImageSize}
import InputConfiguration.defaultTestFraction
import image_classifier.features.ExtractionAlgorithm

private[input] case class InputClass (
		name : String, 
		trainFiles : Seq[String] = Seq[String](), 
		testFiles : Seq[String] = Seq[String](), 
		files : Seq[String] = Seq[String]()
	) {

		require(name.trim.nonEmpty)
		require(files.nonEmpty || trainFiles.nonEmpty)

}

private[input] case class InputConfiguration (
		classes: Seq[InputClass],
		codebookSize : Int = defaultCodebookSize,
		codebookAssignNearest : Boolean = true,
		localFeaturesCount : Int = defaultLocalFeaturesCount,
		localFeaturesAlgorithm : String = defaultLocalFeaturesAlgorithm.toString.toLowerCase,
		maxImageSize : Int = defaultMaxImageSize,
		testFraction: Double = defaultTestFraction,
		testSeed: Option[Int] = None,
	) {

	require(testFraction >= 0 && testFraction <= 1)
	require(classes.nonEmpty)

	val options = InputOptions(codebookSize, codebookAssignNearest, localFeaturesCount, ExtractionAlgorithm.withName(localFeaturesAlgorithm.trim.toUpperCase), maxImageSize)	

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
