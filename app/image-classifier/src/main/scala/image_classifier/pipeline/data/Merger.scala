package image_classifier.pipeline.data

import image_classifier.pipeline.data.Merger.logger
import image_classifier.utils.FileUtils
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.{BytesWritable, IOUtils, IntWritable, SequenceFile}
import org.apache.log4j.Logger
import org.apache.spark.sql.{DataFrame, SparkSession}

private[data] final class Merger(implicit spark: SparkSession) {

	def mergeFiles(files: Seq[(Int, String)], outputFile: String)(implicit fileUtils: FileUtils): Unit =
		mergeBytes(files.map { case (k, v) => (k, fileUtils.readBytes(v)) }, outputFile)

	def mergeBytes(bytes: Seq[(Int, Array[Byte])], outputFile: String): Unit = {
		logger.info(s"Merging ${bytes.length} files into '$outputFile'")
		var writer: SequenceFile.Writer = null
		try {
			val key = new IntWritable
			val value = new BytesWritable
			writer = SequenceFile.createWriter(
				spark.sparkContext.hadoopConfiguration,
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

	def load(file: String, keyCol: String, dataCol: String): DataFrame = {
		import spark.implicits._
		logger.info(s"Loading merged files from '$file'")
		spark.sparkContext.sequenceFile[Int, Array[Byte]](file).toDF(keyCol, dataCol)
	}

}

private[data] object Merger {

	private val logger: Logger = Logger.getLogger(getClass)

}