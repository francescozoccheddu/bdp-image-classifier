package image_classifier.utils

import org.apache.spark.sql.types.{DataType, StructType, StructField}

object StructTypeImplicits {

	implicit final class StructTypeExtension(dataType: DataType) {

		def getField(name: String): Option[StructField] =
			try {
				val struct = dataType.asInstanceOf[StructType]
				Some(struct(name))
			}
			catch {
				case _: Exception => None
			}

		def requireNoField(name: String) =
			require(dataType.getField(name).isEmpty)

		def requireField(name: String, requiredDataType: DataType) = {
			val field = dataType.getField(name)
			require(field.isDefined && field.get.dataType == requiredDataType)
		}

	}

}