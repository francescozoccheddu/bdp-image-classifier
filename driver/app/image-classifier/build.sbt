
scalaVersion := "2.12.13"

libraryDependencies ++= Seq(
	"org.apache.spark" %% "spark-core" % "3.1.1",
	"org.apache.spark" %% "spark-sql" % "3.1.1",
	"org.apache.spark" %% "spark-mllib" % "3.1.1",
	"com.github.pathikrit" %% "dijon" % "0.3.0"
)

