package image_classifier.configuration

object ImageFeatureAlgorithm extends Enumeration {

	type ImageFeatureAlgorithm = Value
	val SIFT = Value("sift")

}

object LogLevel extends Enumeration {

	type LogLevel = Value
	val INFO = Value("info")
	val WARN = Value("warn")
	val ERROR = Value("error")
	val OFF = Value("off")

}

object TrainingAlgorithm extends Enumeration {

	type TrainingAlgorithm = Value
	val NN = Value("nn")

}