package image_classifier.launch

private[launch] object Main {

	def main(args: Array[String]): Unit = {
		val configFile = "/home/fra/Desktop/BD/archive/data.json"
		Launcher.run(configFile)
	}

}
