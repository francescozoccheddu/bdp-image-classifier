package image_classifier.utils

private[image_classifier] object Files {
	import org.apache.hadoop.fs.Path
	import org.apache.log4j.Logger

	import java.time.format.DateTimeFormatter
	import scala.collection.mutable

	def exists(file: String) = {
		import org.apache.hadoop.conf.Configuration
		import org.apache.hadoop.fs.{FileSystem, Path}
		FileSystem.get(new Configuration).exists(new Path(file))
	}

}
