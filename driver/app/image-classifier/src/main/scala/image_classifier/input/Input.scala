package image_classifier.input

import org.apache.spark.rdd.RDD
import org.apache.spark.ml.image.ImageSchema
import org.apache.spark.SparkContext
import com.ctc.wstx.cfg.InputConfigFlags
import org.apache.spark.sql.SparkSession

case class Input private (options : InputOptions, data : RDD[ImageSchema], classes : List[String]) {

    require(classes.nonEmpty)

}

object Input {

    def loadFromConfigFile(configFile : String, spark : SparkSession) : Input = {
        load(InputConfiguration.fromFile(configFile, spark))
    }

    private[input] def load(config : InputConfiguration, spark : SparkSession) : Input = {
        
    }

    

}