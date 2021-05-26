package image_classifier.configuration

object ImageFeatureAlgorithm extends Enumeration {

	type ImageFeatureAlgorithm = Value
	val Sift = Value("sift")

}

object TrainingAlgorithm extends Enumeration {

	type TrainingAlgorithm = Value
	val NaiveBayes = Value("nb")
	val LogisticRegression = Value("lr")
	val DecisionTree = Value("dt")
	val RandomForest = Value("rf")
	val FactorizationMachines = Value("fm")
	val GradientBoosted = Value("gbt")
	val MultilayerPerceptron = Value("mlp")
	val LinearSupportVector = Value("lsv")

}

object LoadMode extends Enumeration {

	type LoadMode = Value
	val Load = Value("load")
	val Make = Value("make")
	val LoadOrMake = Value("loadOrMake")
	val MakeAndSave = Value("makeAndSave")
	val LoadOrMakeAndSave = Value("loadOrMakeAndSave")

}