package image_classifier.input

import InputOptions.{defaultCodebookSize, defaultLocalFeaturesCount}

case class InputOptions private[input] (codebookSize : Int = defaultCodebookSize, localFeaturesCount : Int = defaultLocalFeaturesCount) {

	require(codebookSize > 10 && codebookSize < 1000)
	require(localFeaturesCount > 0 && localFeaturesCount < 100)

}

private[input] object InputOptions {

	val defaultCodebookSize: Int = 500
	val defaultLocalFeaturesCount: Int = 10

}