package image_classifier.pipeline.utils

import image_classifier.pipeline.utils.FileUtils._
import org.apache.spark.scheduler.SparkListener
import org.apache.spark.shuffle.api.WritableByteChannelWrapper
import org.apache.spark.sql.SparkSession

import java.nio.ByteBuffer
import java.nio.channels.Channels
import scala.collection.mutable

private[image_classifier] final class FileUtils(implicit spark: SparkSession) {


	private val sparkListener = new SparkListener {

		import org.apache.spark.scheduler.SparkListenerApplicationEnd

		override def onApplicationEnd(applicationEnd: SparkListenerApplicationEnd) = clearTempFiles()
	}

	spark.sparkContext.removeSparkListener(sparkListener)
	spark.sparkContext.addSparkListener(sparkListener)

	private val tempFiles = mutable.MutableList[String]()

	def addTempFile(file: String): Unit = {
		logger.info(s"Adding temp file '$file'")
		tempFiles += file
	}

	private def clearTempFiles() = {
		logger.info(s"Clearing ${tempFiles.length} temp files")
		import scala.util.Try
		for (file <- tempFiles)
			Try {
				getFs(file).delete(toPath(file), false)
			}
		tempFiles.clear()
	}

	def makeDirs(dir: String) = getFs(dir).mkdirs(toPath(dir))

	def exists(path: String) = getFs(path).exists(toPath(path))

	def readBytes(file: String) = {
		import org.apache.hadoop.io.IOUtils
		val stream = getFs(file).open(toPath(file))
		try IOUtils.readFullyToByteArray(stream)
		finally IOUtils.closeStream(stream)
	}

	def writeBytes(file: String, bytes: Array[Byte]) = {
		import org.apache.hadoop.io.IOUtils
		val stream = getFs(file).create(toPath(file), true)
		try {
			val channel = Channels.newChannel(stream)
			try IOUtils.writeFully(channel, ByteBuffer.wrap(bytes))
			finally IOUtils.closeStream(channel)
		} finally IOUtils.closeStream(stream)
	}

	def writeString(file: String, string: String) = writeBytes(file, string.getBytes)

	def readString(file: String) = new String(readBytes(file))

	private def getFs(path: String) = {
		import java.net.URI
		import org.apache.hadoop.fs.FileSystem
		FileSystem.get(new URI(path), spark.sparkContext.hadoopConfiguration)
	}

}

private[image_classifier] object FileUtils {

	import org.apache.hadoop.fs.Path
	import org.apache.log4j.Logger

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

	def parent(path: String) = {
		val parent = path.getParent
		if (parent == null) "/" else parent.toString
	}

	def isValidLocalPath(path: String) = getIsLocalAndRest(path) match {
		case Some((true, _)) => true
		case _ => false
	}

	def isValidHDFSPath(path: String) = getIsLocalAndRest(path) match {
		case Some((false, _)) => true
		case _ => false
	}

	def isValidPath(path: String) = getIsLocalAndRest(path).isDefined

	def toSimpleLocalPath(path: String) = getIsLocalAndRest(path) match {
		case Some((true, rest)) => rest
		case _ => throw new IllegalArgumentException
	}

	private implicit def toPath(path: String): Path = new Path(path)

	private def getIsLocalAndRest(path: String) = {
		import java.net.URI
		import java.nio.file.{InvalidPathException, Paths}
		val uri = try URI.create(path)
		catch {
			case _: IllegalArgumentException => null
		}
		val (scheme, rest) = (uri.getScheme, uri.getAuthority + uri.getPath)
		val ok = try {
			Paths.get(rest)
			true
		} catch {
			case _: InvalidPathException => false
		}
		if (ok) {
			val isLocal = scheme.trim.toLowerCase match {
				case "file" | "" => Some(true)
				case "hdfs" => Some(false)
				case _ => None
			}
			if (isLocal.isDefined)
				Some((isLocal.get, rest))

			else None
		}
		else
			None
	}

}