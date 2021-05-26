
scalaVersion := "2.12.13"

fork := true

libraryDependencies ++= Seq(
	"org.apache.spark" %% "spark-core" % "3.1.1" % "provided",
	"org.apache.spark" %% "spark-sql" % "3.1.1" % "provided",
	"org.apache.spark" %% "spark-mllib" % "3.1.1" % "provided",
	"com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % "2.8.0",
	"com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "2.8.0" % "provided",
	"com.github.dwickern" %% "scala-nameof" % "3.0.0" % "provided"
	)
