package image_classifier.utils

import org.apache.spark.sql.DataFrame

object DataFrameImplicits {

	implicit final class DataFrameExtension(df : DataFrame) {

		def print(name : String = "DataFrame") = {
			println()
			println(s"${name}[${df.count}]:")
			df.printSchema()
			df.show(20, 100)
		}
		
	}

}