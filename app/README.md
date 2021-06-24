# bdp-image-classifier App

The image classifier is a [Scala](https://www.scala-lang.org/) app for [Apache Spark](https://spark.apache.org/) 3.1.2.
[SBT](https://www.scala-sbt.org/) is used as build tool.  

You can use the [tools](../tools) to build it or to download a prebuilt JAR.

> **NOTE:**  
> [Spark](https://spark.apache.org/) dependency is marked as `provided`, so it will not be included in the output JAR.  
> This means that you need to run it via `spark-submit` or by using the [tools](../tools/).
