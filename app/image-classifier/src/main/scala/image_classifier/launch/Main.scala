package image_classifier.launch

private[launch] object Main {

	def main(args: Array[String]): Unit = {
		require(args.length < 2, "Only the JSON configuration file path argument is accepted")
		require(args.nonEmpty, "The JSON configuration file path argument is required")
		Launcher.run(args.head)
	}

}
