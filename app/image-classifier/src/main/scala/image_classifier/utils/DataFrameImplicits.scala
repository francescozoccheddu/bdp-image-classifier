package image_classifier.utils

import org.apache.spark.sql.DataFrame

private[image_classifier] object DataFrameImplicits {

	implicit final class DataFrameExtension(df: DataFrame) {

		def print(name: String = "DataFrame"): Unit = {
			println()
			println(s"$name[${df.count}]:")
			df.printSchema()
			df.show(20, 100)
		}

	}

}