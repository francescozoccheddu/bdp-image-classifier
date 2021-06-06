package image_classifier.utils

import scala.collection.mutable
import scala.reflect.ClassTag
import scala.reflect.runtime.universe.TypeTag
import scala.util.Try
import java.net.URI
import java.nio.ByteBuffer
import java.nio.channels.Channels
import image_classifier.utils.FileUtils.{isValidPath, logger}
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop.io.{IOUtils, SequenceFile, Writable}
import org.apache.log4j.Logger
import org.apache.spark.WritableConverterWrapper.WritableConverter
import org.apache.spark.scheduler.{SparkListener, SparkListenerApplicationEnd}
import org.apache.spark.sql.{DataFrame, SparkSession}

private[image_classifier] final class FileUtils(val workingDir: String)(implicit spark: SparkSession) {

	logger.info("New instance")

	require(isValidPath(workingDir))
	private val workingPath: Path = new Path(workingDir)

	private val sparkListener: SparkListener = new SparkListener {

		override def onApplicationEnd(applicationEnd: SparkListenerApplicationEnd): Unit = clearTempFiles()
	}

	spark.sparkContext.removeSparkListener(sparkListener)
	spark.sparkContext.addSparkListener(sparkListener)

	private val tempFiles: mutable.MutableList[String] = mutable.MutableList[String]()

	def addTempFile(file: String): Unit = {
		logger.info(s"Adding temp file '$file'")
		tempFiles += resolve(file)
	}

	def makeDirs(dir: String): Boolean = getFs(dir).mkdirs(toPath(dir))

	def createSequenceFileWriter(file: String, keyClass: Class[_ <: Writable], valueClass: Class[_ <: Writable]): SequenceFile.Writer =
		SequenceFile.createWriter(
			spark.sparkContext.hadoopConfiguration,
			SequenceFile.Writer.file(toPath(file)),
			SequenceFile.Writer.keyClass(keyClass),
			SequenceFile.Writer.valueClass(valueClass))

	def loadSequenceFile[Key, Value](file: String, keyCol: String, valueCol: String)(implicit ktt: TypeTag[Key], vtt: TypeTag[Value], kct: ClassTag[Key], vct: ClassTag[Value], kvc: () => WritableConverter[Key], vvc: () => WritableConverter[Value]): DataFrame = {
		import spark.implicits._
		spark.sparkContext.sequenceFile[Key, Value](resolve(file)).toDF(keyCol, valueCol)
	}

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

	private def getFs(path: String): FileSystem = FileSystem.get(URI.create(resolve(path)), spark.sparkContext.hadoopConfiguration)

	def resolve(file: String): String = toPath(file).toString

	private def toPath(path: String): Path = new Path(workingPath, path)

}

private[image_classifier] object FileUtils {

	private val logger: Logger = Logger.getLogger(getClass)

	def parent(path: String): String = {
		val parent = new Path(path).getParent
		if (parent == null) "/" else parent.toString
	}

	def resolve(context: String, path: String): String = new Path(context, path).toString

	def isValidPath(path: String): Boolean = Try {
		URI.create(path)
	}.isSuccess

}