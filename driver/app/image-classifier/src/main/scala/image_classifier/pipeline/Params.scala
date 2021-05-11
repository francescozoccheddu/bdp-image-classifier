package image_classifier.pipeline

import org.apache.spark.ml.param.{Param, Params, IntParam}
import org.apache.spark.ml.param.IntParam

trait HasImageWidthCol extends Params {

	final val imageWidthCol: Param[String] = new Param[String](this, "imageWidthCol", "image width column name")

	setDefault(imageWidthCol, "imageWidth")

	final def getImageWidthCol : String = $(imageWidthCol)

}

trait HasImageHeightCol extends Params {

	final val imageHeightCol: Param[String] = new Param[String](this, "imageHeightCol", "image height column name")

	setDefault(imageHeightCol, "imageHeight")

	final def getImageHeightCol : String = $(imageHeightCol)

}

trait HasImageTypeCol extends Params {

	final val imageTypeCol: Param[String] = new Param[String](this, "imageTypeCol", "image OpenCV type column name")

	setDefault(imageTypeCol, "imageType")

	final def getImageTypeCol : String = $(imageTypeCol)

}

trait HasImageDataCol extends Params {

	final val imageDataCol: Param[String] = new Param[String](this, "imageDataCol", "image data column name")

	setDefault(imageDataCol, "imageData")

	final def getImageDataCol : String = $(imageDataCol)

}