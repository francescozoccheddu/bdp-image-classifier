package image_classifier.utils

private[image_classifier] object OptionImplicits {

	implicit final class OptionExtension[T](option: Option[T]) {

		def getOr(func: () => T) =
			if (option.isDefined)
				option.get
			else
				func()

	}

}