package image_classifier.input

import org.apache.spark.sql.{SparkSession, DataFrame}


object Merger {

	private def readFile(file : String) : Array[Byte] = {
		import java.nio.file.{Files, Paths}
		Files.readAllBytes(Paths.get(file))
	}

	def mergeFiles(files : Seq[(Int, String)], outputFile : String) : Unit = 
		mergeBytes(files.map{ case (k, v) => (k, readFile(v)) }, outputFile)		
	
	def mergeBytes(bytes : Seq[(Int, Array[Byte])], outputFile : String) : Unit = {
		import org.apache.hadoop.io.{SequenceFile, BytesWritable, IntWritable, IOUtils}
		import org.apache.hadoop.fs.Path
		import org.apache.hadoop.conf.Configuration

		var writer : SequenceFile.Writer = null
        try{
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

	def load(spark : SparkSession, file : String, keyColumn : String, valueColumn : String) : DataFrame = {
		import spark.implicits._
		spark.sparkContext.sequenceFile[Int, Array[Byte]](file).toDF(keyColumn, valueColumn)
	}

}