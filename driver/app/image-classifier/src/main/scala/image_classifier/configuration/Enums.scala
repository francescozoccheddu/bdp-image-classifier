package image_classifier.configuration

object ImageFeatureAlgorithm extends Enumeration {

	type ImageFeatureAlgorithm = Value
	val Sift = Value("sift")

}

object LogLevel extends Enumeration {

	type LogLevel = Value
	val Info = Value("info")
	val Warn = Value("warn")
	val Error = Value("error")
	val Off = Value("off")

}

object TrainingAlgorithm extends Enumeration {

	type TrainingAlgorithm = Value
	val NearestNeighbor = Value("nn")

}

object LoadMode extends Enumeration {

	type LoadMode = Value
	val Load = Value("load")
	val Make = Value("make")
	val LoadOrMake = Value("loadOrMake")
	val MakeAndSave = Value("makeAndSave")
	val LoadOrMakeAndSave = Value("loadOrMakeAndSave")

}