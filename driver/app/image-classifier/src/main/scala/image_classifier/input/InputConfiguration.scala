package image_classifier.input

import InputOptions.{defaultCodebookSize, defaultLocalFeaturesCount}
import InputConfiguration.defaultTestFraction

private[input] case class InputConfiguration (classes : List[InputClass], codebookSize : Int = defaultCodebookSize, localFeaturesCount : Int = defaultLocalFeaturesCount, testFraction : Double = defaultTestFraction, testSeed : Option[Int] = None) {

	require(classes.nonEmpty)
	require(codebookSize > 10 && codebookSize < 1000)
	require(localFeaturesCount > 0 && localFeaturesCount < 100)
	require(testFraction >= 0 && testFraction <= 1)

	def toOptions : InputOptions = InputOptions(codebookSize, localFeaturesCount)

}

private[input] object InputConfiguration {

	val defaultTestFraction: Double = 0.2

	def fromFile(configFile: String): InputConfiguration = {
		import scala.io.Source
		val source = Source.fromFile(configFile)
		val json = try source.mkString finally source.close
		fromJSON(json)
	}

	private def fromJsonString(configJsonString : String) : InputConfiguration = {
		import com.github.pathikrit.dijon._
		fromJSON(parse(configJsonString))
	}

	import com.github.pathikrit.dijon.SomeJson

	private def fromJSON(configJson : SomeJson) : InputConfiguration = {
		import scala.language.dynamics._
		val codebookSize = configJson.codebookSize.asInt.getOrElse(defaultCodebookSize)
		val localFeaturesCount = configJson.localFeaturesCount.asInt.getOrElse(defaultLocalFeaturesCount)
		val testFraction = configJson.testFraction.asDouble.getOrElse(defaultTestFraction)
		val testSeed = configJson.testFraction.asInt
		val classes = configJson.classes.toSeq.map(classFromJSON).toList
		new InputConfiguration(classes, codebookSize, localFeaturesCount, testFraction, testSeed)
	}

	private def classFromJSON(classJson : SomeJson) : InputClass = {
		val name = classJson.name.asString.get
		val trainFiles = classJson.trainFiles.toSeq.map(_.asString.get).toList
		val testFiles = classJson.trainFiles.toSeq.map(_.asString.get).toList
		val mergedFiles = classJson.trainFiles.toSeq.map(_.asString.get).toList
		InputClass(name, trainFiles, testFiles, mergedFiles)
	}

}