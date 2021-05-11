package image_classifier.utils

import org.apache.spark.sql.types.{DataType, StructType, StructField}

object StructTypeImplicits {

	implicit class StructTypeExtension(dataType : DataType) {

		def getField(name : String) : Option[StructField] =
			try {
				val struct = dataType.asInstanceOf[StructType]
				Some(struct.fields(struct.fieldIndex(name))) 
			} 
			catch { case _ : Exception => None }

		def requireNoField(name : String) = 
			require(dataType.getField(name).isEmpty)
		
		def requireField(name : String, dataType : DataType) = 
		{
			val field = dataType.getField(name)
			require(field.isDefined && field.get.dataType == dataType)
		}
		
	}

}