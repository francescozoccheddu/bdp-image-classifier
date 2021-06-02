package image_classifier.utils

import scala.collection.mutable
import scala.util.Try
import java.net.URI
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.file.{InvalidPathException, Paths}
import image_classifier.utils.FileUtils.logger
import org.apache.hadoop.fs.{FileSystem, LocalFileSystem, Path}
import org.apache.hadoop.io.IOUtils
import org.apache.log4j.Logger
import org.apache.spark.scheduler.{SparkListener, SparkListenerApplicationEnd}
import org.apache.spark.sql.SparkSession

private[image_classifier] final class FileUtils(val workingDir: String)(implicit spark: SparkSession) {

	logger.info("New instance")

	require(FileUtils.isValidLocalPath(workingDir))

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
		tempFiles += resolve(file)
	}

	def resolve(file: String): String = FileUtils.resolve(workingDir, file)

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

	def glob(glob: String): Seq[String] =
		getFs(glob).globStatus(toPath(glob)).map(_.getPath.toString)

	private def clearTempFiles(): Unit = {
		logger.info(s"Clearing ${tempFiles.length} temp files")
		for (file <- tempFiles)
			Try {
				getFs(file).delete(toPath(file), false)
			}
		tempFiles.clear()
	}

	private def getFs(path: String): FileSystem = FileUtils.getIsLocalAndRest(path) match {
		case Some((true, _)) => localFs
		case Some((false, _)) => hdfs
		case _ => throw new IllegalArgumentException
	}

	private def toPath(path: String): Path = new Path(workingDir, path)

}

private[image_classifier] object FileUtils {

	private val logger: Logger = Logger.getLogger(getClass)

	def parent(path: String): String = {
		val parent = new Path(path).getParent
		if (parent == null) "/" else parent.toString
	}

	def resolve(context: String, path: String): String = new Path(context, path).toString

	def isValidLocalPath(path: String): Boolean = getIsLocalAndRest(path) match {
		case Some((true, _)) => true
		case _ => false
	}

	def isValidHDFSPath(path: String): Boolean = getIsLocalAndRest(path) match {
		case Some((false, _)) => true
		case _ => false
	}

	def isValidPath(path: String): Boolean = getIsLocalAndRest(path).isDefined

	def toSimpleLocalPath(path: String): String = getIsLocalAndRest(path) match {
		case Some((true, rest)) => rest
		case _ => throw new IllegalArgumentException
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

}