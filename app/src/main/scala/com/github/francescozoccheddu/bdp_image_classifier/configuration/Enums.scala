package com.github.francescozoccheddu.bdp_image_classifier.configuration

import scala.language.implicitConversions
import com.github.francescozoccheddu.bdp_image_classifier.configuration

object ImageFeatureAlgorithm extends Enumeration {

	type ImageFeatureAlgorithm = Value
	val Sift: configuration.ImageFeatureAlgorithm.Value = Value("sift")
	val Surf: configuration.ImageFeatureAlgorithm.Value = Value("surf")
	val Orb: configuration.ImageFeatureAlgorithm.Value = Value("orb")

}

object TrainingAlgorithm extends Enumeration {

	type TrainingAlgorithm = Value
	val NaiveBayes: configuration.TrainingAlgorithm.Value = Value("nb")
	val LogisticRegression: configuration.TrainingAlgorithm.Value = Value("lr")
	val DecisionTree: configuration.TrainingAlgorithm.Value = Value("dt")
	val RandomForest: configuration.TrainingAlgorithm.Value = Value("rf")
	val FactorizationMachines: configuration.TrainingAlgorithm.Value = Value("fm")
	val GradientBoosted: configuration.TrainingAlgorithm.Value = Value("gbt")
	val MultilayerPerceptron: configuration.TrainingAlgorithm.Value = Value("mlp")
	val LinearSupportVector: configuration.TrainingAlgorithm.Value = Value("lsv")
	val NearestNeighbor: configuration.TrainingAlgorithm.Value = Value("nn")

}

object LoadMode extends Enumeration {

	type LoadMode = Value

	implicit def extend(value: LoadMode): LoadModeExtension = new LoadModeExtension(value)

	val Load: configuration.LoadMode.Value = Value("load")
	val Make: configuration.LoadMode.Value = Value("make")
	val LoadOrMake: configuration.LoadMode.Value = Value("loadOrMake")
	val MakeAndSave: configuration.LoadMode.Value = Value("makeAndSave")
	val LoadOrMakeAndSave: configuration.LoadMode.Value = Value("loadOrMakeAndSave")

	final class LoadModeExtension(value: Value) {
		def canMake: Boolean = value match {
			case LoadOrMake | Make | MakeAndSave => true
			case _ => false
		}

		def canLoad: Boolean = value match {
			case Load | LoadOrMake | LoadOrMakeAndSave => true
			case _ => false
		}

		def canSave: Boolean = value match {
			case LoadOrMakeAndSave | MakeAndSave => true
			case _ => false
		}

		def alwaysDoesMake: Boolean = value match {
			case Make | MakeAndSave => true
			case _ => false
		}
	}

}