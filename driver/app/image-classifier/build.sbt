
scalaVersion := "2.12.13"

fork := true

libraryDependencies ++= Seq(
	"org.apache.spark" %% "spark-core" % "3.1.1",
	"org.apache.spark" %% "spark-sql" % "3.1.1",
	"org.apache.spark" %% "spark-mllib" % "3.1.1",
  	"com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core"   % "2.8.0",
  	"com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "2.8.0" % "compile-internal"
)

// TODO Setup for spark-submit
