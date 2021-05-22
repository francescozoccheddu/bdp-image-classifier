package image_classifier.pipeline

object Pipeline {
	import image_classifier.configuration.Config
	import image_classifier.pipeline.utils.Columns.colName
	import org.apache.spark.sql.SparkSession
	import org.apache.log4j.Logger

	private val logger = Logger.getLogger(Pipeline.getClass)

	private val isTestCol = colName("isTest")
	private val dataCol = colName("data")
	private val labelCol = colName("label")
	private val predictionCol = colName("prediction")

	def test(spark: SparkSession) = {
		import image_classifier.pipeline.utils.NearestNeighbor
		import org.apache.spark.ml.linalg.{Vector => MLVector}
		import spark.implicits._
		import image_classifier.utils.DataFrameImplicits._
		import org.apache.spark.sql.Row
		val nn = new NearestNeighbor("features")
		var (approxSum, naiveSum) = (0L, 0L)
		val iterations = 10
		for (i <- 1 to iterations) {
			println(s"ITERATION $i")
			val test = {
				import org.apache.spark.ml.linalg.Vectors
				for (x <- 0 to 9; y <- 0 to 9; z <- 0 to 9)
					yield Vectors.dense(x, y, z)
			}
			val testDF = test.map(Tuple1.apply).toDF("features").cache
			val keyDF = Seq.fill(100) {
				import org.apache.spark.ml.linalg.Vectors
				import scala.util.Random.nextDouble
				Vectors.dense(nextDouble * 10, nextDouble * 10, nextDouble * 10)
			}.map(Tuple1.apply).toDF("features").cache
			testDF.print()
			keyDF.print()

			{
				val time = System.nanoTime
				nn.approxJoin(keyDF, testDF, (t: Row) => t.getAs[MLVector](0)).print()
				approxSum += (System.nanoTime - time)
			}
			{
				val time = System.nanoTime
				nn.naiveJoin(keyDF, test, (t: MLVector) => t).print()
				naiveSum += (System.nanoTime - time)
			}
		}
		println()
		println(s"Approx ${approxSum.toDouble / iterations / 1e9d}s")
		println(s"Naive ${naiveSum.toDouble / iterations / 1e9d}s")
		sys.exit()
	}

	def run(config: Config, workingDir: String)(implicit spark: SparkSession): Unit = {
		test(spark)
		import image_classifier.pipeline.data.DataStage
		import image_classifier.pipeline.featurization.FeaturizationStage
		import image_classifier.pipeline.testing.TestingStage
		import image_classifier.pipeline.training.TrainingStage
		import image_classifier.utils.DataFrameImplicits._
		logger.info("Pipeline started")
		val data = new DataStage(config.data, workingDir, labelCol, isTestCol, dataCol)
		val featurization = new FeaturizationStage(config.featurization, data, dataCol)
		val training = new TrainingStage(config.training, featurization, predictionCol)
		val testing = new TestingStage(config.testing, training)
		Seq(testing, training, featurization, data).takeWhile(!_.hasResult)
		logger.info("Pipeline ended")
	}

}
