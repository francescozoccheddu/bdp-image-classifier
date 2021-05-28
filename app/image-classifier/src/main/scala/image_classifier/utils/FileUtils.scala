package image_classifier.utils

import scala.collection.mutable
import scala.jdk.CollectionConverters.iterableAsScalaIterableConverter
import scala.util.Try
import java.io.File
import java.net.URI
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.file.{Files, InvalidPathException, Paths}
import image_classifier.utils.FileUtils._
import org.apache.hadoop.fs.{FileSystem, LocalFileSystem, Path}
import org.apache.hadoop.io.IOUtils
import org.apache.log4j.Logger
import org.apache.spark.scheduler.{SparkListener, SparkListenerApplicationEnd}
import org.apache.spark.sql.SparkSession

private[image_classifier] final class FileUtils(implicit spark: SparkSession) {

	logger.info("New instance")

	private val sparkListener: SparkListener = new SparkListener {

		override def onApplicationEnd(applicationEnd: SparkListenerApplicationEnd): Unit = clearTempFiles()
	}

	spark.sparkContext.removeSparkListener(sparkListener)
	spark.sparkContext.addSparkListener(sparkListener)

	private val tempFiles: mutable.MutableList[String] = mutable.MutableList[String]()
	private val hdfs: FileSystem = FileSystem.get(spark.sparkContext.hadoopConfiguration)
	private val localFs: LocalFileSystem = FileSystem.getLocal(spark.sparkContext.hadoopConfiguration)

	def addTempFile(file: String): Unit = {
		logger.info(s"Adding temp file '$file'")
		tempFiles += file
	}

	def makeDirs(dir: String): Boolean = getFs(dir).mkdirs(toPath(dir))

	def exists(path: String): Boolean = getFs(path).exists(toPath(path))

	def writeString(file: String, string: String): Unit = writeBytes(file, string.getBytes)

	def writeBytes(file: String, bytes: Array[Byte]): Unit = {
		val stream = getFs(file).create(toPath(file), true)
		try {
			val channel = Channels.newChannel(stream)
			try IOUtils.writeFully(channel, ByteBuffer.wrap(bytes))
			finally IOUtils.closeStream(channel)
		} finally IOUtils.closeStream(stream)
	}

	def readString(file: String): String = new String(readBytes(file))

	def readBytes(file: String): Array[Byte] = {
		val stream = getFs(file).open(toPath(file))
		try IOUtils.readFullyToByteArray(stream)
		finally IOUtils.closeStream(stream)
	}

	private def clearTempFiles(): Unit = {
		logger.info(s"Clearing ${tempFiles.length} temp files")
		for (file <- tempFiles)
			Try {
				getFs(file).delete(toPath(file), false)
			}
		tempFiles.clear()
	}

	private def getFs(path: String): FileSystem = getIsLocalAndRest(path) match {
		case Some((true, _)) => localFs
		case Some((false, _)) => hdfs
		case _ => throw new IllegalArgumentException
	}

}

private[image_classifier] object FileUtils {

	private val logger: Logger = Logger.getLogger(getClass)

	def listFiles(workingDir: String, glob: String): Seq[String] = {
		val (globHead, globTail) = {
			val index = glob.lastIndexWhere(c => c == '\\' || c == '/' || c == File.separatorChar)
			if (index > 0) (glob.substring(0, index + 1), glob.substring(index + 1))
			else (".", glob)
		}
		val head = if (URI.create(globHead).isAbsolute) globHead else workingDir + File.separator + globHead
		val stream = Files.newDirectoryStream(Paths.get(head), globTail)
		try stream.asScala.map(_.normalize().toString).toSeq.sorted
		finally if (stream != null) stream.close()
	}

	def parent(path: String): String = {
		val parent = toPath(path).getParent
		if (parent == null) "/" else parent.toString
	}

	private def toPath(path: String): Path = new Path(path)

	def isValidLocalPath(path: String): Boolean = getIsLocalAndRest(path) match {
		case Some((true, _)) => true
		case _ => false
	}

	def isValidHDFSPath(path: String): Boolean = getIsLocalAndRest(path) match {
		case Some((false, _)) => true
		case _ => false
	}

	private def getIsLocalAndRest(path: String): Option[(Boolean, String)] = {
		val uri = try URI.create(path)
		catch {
			case _: IllegalArgumentException => null
		}
		val (scheme, rest) = (if (uri.getScheme != null) uri.getScheme else "", uri.getAuthority + uri.getPath)
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

	def isValidPath(path: String): Boolean = getIsLocalAndRest(path).isDefined

	def toSimpleLocalPath(path: String): String = getIsLocalAndRest(path) match {
		case Some((true, rest)) => rest
		case _ => throw new IllegalArgumentException
	}

}