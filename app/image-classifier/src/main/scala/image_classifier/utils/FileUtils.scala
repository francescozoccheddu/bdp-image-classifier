package image_classifier.utils

import scala.collection.mutable
import scala.language.implicitConversions
import scala.reflect.ClassTag
import scala.reflect.runtime.universe.TypeTag
import scala.util.Try
import java.nio.ByteBuffer
import java.nio.channels.Channels
import image_classifier.utils.FileUtils.logger
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop.io.{IOUtils, SequenceFile, Writable}
import org.apache.log4j.Logger
import org.apache.spark.WritableConverterWrapper.WritableConverter
import org.apache.spark.scheduler.{SparkListener, SparkListenerApplicationEnd}
import org.apache.spark.sql.{DataFrame, SparkSession}

private[image_classifier] final class FileUtils(val workingDir: String)(implicit spark: SparkSession) {

	private implicit class PathExtension(string: String) {

		val path: Path = new Path(workingPath, string)

		def run[T](func: (Path, FileSystem) => T): T = func(path, fs)

		def fs: FileSystem = path.getFileSystem(spark.sparkContext.hadoopConfiguration)

	}

	logger.info("New instance")

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

	def makeDirs(dir: String): Boolean = dir.run { case (path, fs) => fs.mkdirs(path) }

	def createSequenceFileWriter(file: String, keyClass: Class[_ <: Writable], valueClass: Class[_ <: Writable]): SequenceFile.Writer =
		SequenceFile.createWriter(
			spark.sparkContext.hadoopConfiguration,
			SequenceFile.Writer.file(file.path),
			SequenceFile.Writer.keyClass(keyClass),
			SequenceFile.Writer.valueClass(valueClass))

	def loadSequenceFile[Key, Value](file: String, keyCol: String, valueCol: String)(implicit ktt: TypeTag[Key], vtt: TypeTag[Value], kct: ClassTag[Key], vct: ClassTag[Value], kvc: () => WritableConverter[Key], vvc: () => WritableConverter[Value]): DataFrame = {
		import spark.implicits._
		spark.sparkContext.sequenceFile[Key, Value](resolve(file)).toDF(keyCol, valueCol)
	}

	def resolve(file: String): String = file.path.toString

	def exists(path: String): Boolean = path.run { case (path, fs) => fs.exists(path) }

	def writeString(file: String, string: String): Unit = writeBytes(file, string.getBytes)

	def writeBytes(file: String, bytes: Array[Byte]): Unit = {
		val stream = file.run { case (path, fs) => fs.create(path, true) }
		try {
			val channel = Channels.newChannel(stream)
			try IOUtils.writeFully(channel, ByteBuffer.wrap(bytes))
			finally IOUtils.closeStream(channel)
		} finally IOUtils.closeStream(stream)
	}

	def readString(file: String): String = new String(readBytes(file))

	def readBytes(file: String): Array[Byte] = {
		val stream = file.run { case (path, fs) => fs.open(path) }
		try IOUtils.readFullyToByteArray(stream)
		finally IOUtils.closeStream(stream)
	}

	def glob(glob: String): Seq[String] =
		glob.run { case (path, fs) => fs.globStatus(path).map(_.getPath.toString) }

	private def clearTempFiles(): Unit = {
		logger.info(s"Clearing ${tempFiles.length} temp files")
		for (file <- tempFiles) Try {
			file.run { case (path, fs) => fs.delete(path, false) }
		}
		tempFiles.clear()
	}

}

private[image_classifier] object FileUtils {

	private val logger: Logger = Logger.getLogger(getClass)

	def parent(path: String): String = {
		val parent = new Path(path).getParent
		if (parent == null) "/" else parent.toString
	}

	def resolve(context: String, path: String): String = new Path(context, path).toString

}