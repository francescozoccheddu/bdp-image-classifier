package com.github.francescozoccheddu.bdp_image_classifier.pipeline.data

import com.github.francescozoccheddu.bdp_image_classifier.pipeline.data.Merger.logger
import com.github.francescozoccheddu.bdp_image_classifier.utils.FileUtils
import org.apache.hadoop.io.{BytesWritable, IOUtils, IntWritable, SequenceFile}
import org.apache.log4j.Logger
import org.apache.spark.sql.DataFrame

private[data] final class Merger(implicit fileUtils: FileUtils) {

	def mergeFiles(files: Seq[(Int, String)], outputFile: String): Unit =
		mergeBytes(files.map { case (k, v) => (k, fileUtils.readBytes(v)) }, outputFile)

	def mergeBytes(bytes: Seq[(Int, Array[Byte])], outputFile: String): Unit = {
		logger.info(s"Merging ${bytes.length} files into '$outputFile'")
		var writer: SequenceFile.Writer = null
		try {
			val key = new IntWritable
			val value = new BytesWritable
			writer = fileUtils.createSequenceFileWriter(outputFile, key.getClass, value.getClass)
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
		logger.info(s"Loading merged files from '$file'")
		fileUtils.loadSequenceFile[Int, Array[Byte]](file, keyCol, dataCol)
	}

}

private[data] object Merger {

	private val logger: Logger = Logger.getLogger(getClass)

}