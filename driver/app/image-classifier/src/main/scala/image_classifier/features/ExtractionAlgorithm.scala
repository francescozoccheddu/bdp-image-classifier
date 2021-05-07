package image_classifier.features

object ExtractionAlgorithm extends Enumeration {
	
  type ExtractionAlgorithm = Value
  
  val SIFT, SURF, ORB, MSER, FAST, GFTT = Value

}