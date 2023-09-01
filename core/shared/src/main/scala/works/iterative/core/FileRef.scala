package works.iterative.core

/** Represents a reference to a file */
case class FileRef private (
    name: String,
    url: String,
    fileType: Option[String],
    size: Option[String]
)

object FileRef:
  def apply(
      name: String,
      url: String,
      fileType: Option[String] = None,
      size: Option[String] = None
  ): Validated[FileRef] =
    for
      name <- Validated.nonEmptyString("file.name")(name)
      url <- Validated.nonEmptyString("file.url")(url)
    yield new FileRef(name, url, fileType, size)

  def unsafe(
      name: String,
      url: String,
      fileType: Option[String] = None,
      size: Option[String] = None
  ): FileRef = new FileRef(name, url, fileType, size)
