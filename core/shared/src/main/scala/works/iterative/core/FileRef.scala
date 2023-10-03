package works.iterative.core

/** Represents a reference to a file */
case class FileRef private (
    name: String,
    url: String,
    fileType: Option[String],
    size: Option[Long]
):
  def sizeString: Option[String] =
    size.map {
      case s if s < 1024               => s"$s B"
      case s if s < 1024 * 1024        => s"${s / 1024} KB"
      case s if s < 1024 * 1024 * 1024 => s"${s / (1024 * 1024)} MB"
      case s                           => s"${s / (1024 * 1024 * 1024)} GB"
    }

  def withBase(url: String): FileRef = copy(url = url + this.url)

object FileRef:
  def apply(
      name: String,
      url: String,
      fileType: Option[String] = None,
      size: Option[Long] = None
  ): Validated[FileRef] =
    for
      name <- Validated.nonEmptyString("file.name")(name)
      url <- Validated.nonEmptyString("file.url")(url)
    yield new FileRef(name, url, fileType, size)

  def unsafe(
      name: String,
      url: String,
      fileType: Option[String] = None,
      size: Option[Long] = None
  ): FileRef = new FileRef(name, url, fileType, size)
