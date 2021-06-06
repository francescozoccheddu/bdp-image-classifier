
scalaVersion := "2.12.13"

fork := true

val sparkVersion: String = "3.1.1"

libraryDependencies ++= Seq(
	"org.apache.spark" %% "spark-core" % sparkVersion % "provided",
	"org.apache.spark" %% "spark-sql" % sparkVersion % "provided",
	"org.apache.spark" %% "spark-mllib" % sparkVersion % "provided",
	"com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % "2.8.0",
	"com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "2.8.0" % "provided",
	"com.github.dwickern" %% "scala-nameof" % "3.0.0" % "provided"
)
