package image_classifier.pipeline.data

import image_classifier.utils.FileUtils
import org.apache.spark.sql.{DataFrame, SparkSession}

private[data] object Merger {
	import org.apache.log4j.Logger

	private val logger = Logger.getLogger(getClass)

	def mergeFiles(files: Seq[(Int, String)], outputFile: String)(implicit fileUtils: FileUtils): Unit =
		mergeBytes(files.map { case (k, v) => (k, fileUtils.readBytes(v)) }, outputFile)

	def mergeBytes(bytes: Seq[(Int, Array[Byte])], outputFile: String): Unit = {
		import org.apache.hadoop.io.{SequenceFile, BytesWritable, IntWritable, IOUtils}
		import org.apache.hadoop.fs.Path
		import org.apache.hadoop.conf.Configuration
		logger.info(s"Merging ${bytes.length} files into '$outputFile'")
		var writer: SequenceFile.Writer = null
		try {
			val config = new Configuration
			val key = new IntWritable
			val value = new BytesWritable
			writer = SequenceFile.createWriter(
				config,
				SequenceFile.Writer.file(new Path(outputFile)),
				SequenceFile.Writer.keyClass(key.getClass),
				SequenceFile.Writer.valueClass(value.getClass))
			for ((k, v) <- bytes) {
				key.set(k)
				value.set(v, 0, v.length)
				writer.append(key, value)
			}
			writer.close()
			writer = null
		} finally {
			if (writer != null)
				IOUtils.closeStream(writer)
		}
	}

	def load(file: String, keyCol: String, dataCol: String)(implicit spark: SparkSession): DataFrame = {
		import spark.implicits._
		logger.info(s"Loading merged files from '$file'")
		spark.sparkContext.sequenceFile[Int, Array[Byte]](file).toDF(keyCol, dataCol)
	}

}