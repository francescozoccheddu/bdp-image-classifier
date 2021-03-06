package com.github.francescozoccheddu.bdp_image_classifier.pipeline.utils

import org.apache.spark.sql.DataFrame

private[pipeline] object DataFrameImplicits {

	implicit final class DataFrameExtension(df: DataFrame) {

		def print(name: String = "DataFrame"): Unit = {
			println()
			println(s"$name[${df.count}]:")
			df.printSchema()
			df.show(20, 100)
		}

	}

}