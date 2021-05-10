package image_classifier.input

import InputOptions.{defaultCodebookSize, defaultCodebookAssignNearest, defaultLocalFeaturesCount, defaultMaxImageSize}

case class InputOptions private[input] (
	codebookSize : Int = defaultCodebookSize, 
	codebookAssignNearest : Boolean = defaultCodebookAssignNearest,
	localFeaturesCount : Int = defaultLocalFeaturesCount, 
	maxImageSize : Int = defaultMaxImageSize) {

	require(codebookSize >= 10 && codebookSize < 5000)
	require(localFeaturesCount > 0 && localFeaturesCount < 1000)
	require(maxImageSize >= 4 && maxImageSize <= 8192)

}

private[input] object InputOptions {

	val defaultCodebookSize : Int = 500
	val defaultLocalFeaturesCount : Int = 10
	val defaultMaxImageSize : Int = 512
	val defaultCodebookAssignNearest : Boolean = true

}