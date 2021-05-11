package image_classifier.pipeline

import org.apache.spark.ml.param.{Param, Params, IntParam}

trait HasImageWidthCol extends Params {

	final val imageWidthCol: Param[String] = new Param[String](this, "imageWidthCol", "image width column name")

	setDefault(imageWidthCol, "imageWidth")

	final def getImageWidthCol : String = $(imageWidthCol)

	final def setImageWidthCol(value: String): this.type = set(imageWidthCol, value).asInstanceOf[this.type]
	
}

trait HasImageHeightCol extends Params {
	
	final val imageHeightCol: Param[String] = new Param[String](this, "imageHeightCol", "image height column name")
	
	setDefault(imageHeightCol, "imageHeight")
	
	final def getImageHeightCol : String = $(imageHeightCol)
	
	final def setImageHeightCol(value: String): this.type = set(imageHeightCol, value).asInstanceOf[this.type]
	
}

trait HasImageTypeCol extends Params {
	
	final val imageTypeCol: Param[String] = new Param[String](this, "imageTypeCol", "image OpenCV type column name")
	
	setDefault(imageTypeCol, "imageType")
	
	final def getImageTypeCol : String = $(imageTypeCol)
	
	final def setImageTypeCol(value: String): this.type = set(imageTypeCol, value).asInstanceOf[this.type]
	
}

trait HasImageDataCol extends Params {
	
	final val imageDataCol: Param[String] = new Param[String](this, "imageDataCol", "image data column name")
	
	setDefault(imageDataCol, "imageData")
	
	final def getImageDataCol : String = $(imageDataCol)
	
	final def setImageDataCol(value: String): this.type = set(imageDataCol, value).asInstanceOf[this.type]

}

trait HasFeaturesCount extends Params {
	
	final val featuresCount: IntParam = new IntParam(this, "featuresCount", "features count", _ > 0)
	
	setDefault(featuresCount, 10)
	
	final def getFeaturesCount : Int = $(featuresCount)
	
	final def setFeaturesCount(value: Int): this.type = set(featuresCount, value).asInstanceOf[this.type]

}