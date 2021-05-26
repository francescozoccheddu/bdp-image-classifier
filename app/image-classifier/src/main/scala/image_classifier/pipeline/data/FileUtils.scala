package image_classifier.pipeline.data

private[data] object FileUtils {
	import org.apache.hadoop.fs.Path
	import org.apache.log4j.Logger

	import java.time.format.DateTimeFormatter
	import scala.collection.mutable

	private val tempFiles: mutable.MutableList[String] = mutable.MutableList()
	private val dateFormat = DateTimeFormatter.ofPattern("yy-MM-dd-HH-mm-ss-SSS")
	private val logger = Logger.getLogger(getClass)

	def listFiles(workingDir: String, glob: String): Seq[String] = {
		import java.io.File
		import java.net.URI
		val (globHead, globTail) = {
			val index = glob.lastIndexWhere(c => c == '\\' || c == '/' || c == File.separatorChar)
			if (index > 0) (glob.substring(0, index + 1), glob.substring(index + 1))
			else (".", glob)
		}
		val head = if (URI.create(globHead).isAbsolute) globHead else workingDir + File.separator + globHead
		import java.nio.file.{Files, Paths}
		import scala.jdk.CollectionConverters.iterableAsScalaIterableConverter
		val stream = Files.newDirectoryStream(Paths.get(head), globTail)
		try stream.asScala.map(_.normalize().toString).toSeq.sorted
		finally if (stream != null) stream.close()
	}

	private def makeTempFilePath = {
		import java.time.LocalDateTime
		import java.util.UUID.randomUUID
		new Path(s"tmp_${LocalDateTime.now.format(dateFormat)}_$randomUUID")
	}

	def getTempHdfsFile(dir: String): String = {
		import org.apache.hadoop.conf.Configuration
		import org.apache.hadoop.fs.{FileSystem, Path}
		val dirPath = new Path(dir)
		FileSystem.get(new Configuration).mkdirs(dirPath)
		val filePath = new Path(dirPath, makeTempFilePath)
		addTempFile(filePath.toString)
		filePath.toString
	}

	def addTempFile(file: String): Unit = {
		logger.info(s"Adding temp file '$file'")
		tempFiles += file
	}

	def clearTempFiles(): Unit = {
		import org.apache.hadoop.conf.Configuration
		import org.apache.hadoop.fs.FileSystem
		logger.info(s"Clearing ${tempFiles.length} temp files")
		val fs = FileSystem.get(new Configuration)
		import scala.util.Try
		for (file <- tempFiles) {
			Try {
				fs.delete(new Path(file), false)
			}
		}
		tempFiles.clear()
	}

}
