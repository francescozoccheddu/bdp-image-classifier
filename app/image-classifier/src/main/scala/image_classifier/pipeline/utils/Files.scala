package image_classifier.pipeline.utils

private[pipeline] object Files {

	def exists(file: String) = {
		import org.apache.hadoop.conf.Configuration
		import org.apache.hadoop.fs.{FileSystem, Path}
		FileSystem.get(new Configuration).exists(new Path(file))
	}

}
