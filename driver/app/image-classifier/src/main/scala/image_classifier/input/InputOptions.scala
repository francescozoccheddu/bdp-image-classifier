package image_classifier.input

import InputOptions.{defaultCodebookSize, defaultLocalFeaturesCount, defaultLocalFeaturesAlgorithm, defaultMaxImageSize}
import image_classifier.features.ExtractionAlgorithm._
import image_classifier.features.ExtractionAlgorithm

case class InputOptions private[input] (
	codebookSize : Int = defaultCodebookSize, 
	localFeaturesCount : Int = defaultLocalFeaturesCount, 
	localFeaturesAlgorithm : ExtractionAlgorithm = defaultLocalFeaturesAlgorithm,
	maxImageSize : Int = defaultMaxImageSize) {

	require(codebookSize >= 10 && codebookSize < 5000)
	require(localFeaturesCount > 0 && localFeaturesCount < 1000)
	require(maxImageSize >= 4 && maxImageSize <= 8192)

}

private[input] object InputOptions {

	val defaultCodebookSize : Int = 500
	val defaultLocalFeaturesCount : Int = 10
	val defaultLocalFeaturesAlgorithm : ExtractionAlgorithm = ExtractionAlgorithm.SIFT
	val defaultMaxImageSize : Int = 512

}