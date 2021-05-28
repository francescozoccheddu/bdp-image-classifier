package image_classifier.pipeline

private[pipeline] object Columns {

	private val reservedPrefix: String = "_res"
	private var count: Int = 0

	def colName(name: String): String = {
		require(!name.startsWith(reservedPrefix))
		name
	}

	def resColName(name: String): String = {
		count += 1
		s"$reservedPrefix${count}_$name"
	}

}
