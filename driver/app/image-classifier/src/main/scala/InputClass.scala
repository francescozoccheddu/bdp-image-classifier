package com.fra

import org.apache.spark.rdd.RDD

case class InputClass(name : String, trainFiles : RDD[String], testFiles : RDD[String]) {

	require(name.trim.nonEmpty)
	require(!trainFiles.isEmpty)
	require(!testFiles.isEmpty)

}
