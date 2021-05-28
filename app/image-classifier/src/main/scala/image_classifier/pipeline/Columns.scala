package image_classifier.pipeline

private[pipeline] object Columns {

	private val reservedPrefix = "_res"
	private var count = 0

	def colName(name: String) = {
		require(!name.startsWith(reservedPrefix))
		name
	}

	def resColName(name: String) = {
		count += 1
		s"$reservedPrefix${count}_$name"
	}

}
