package works.iterative.core

import service.FileStore

/** Represents a reference to a file */
case class FileRef(
    name: String,
    url: String,
    metadata: FileStore.Metadata
):
  def fileType = metadata.get(FileStore.Metadata.FileType)
  def size = metadata.get(FileStore.Metadata.Size).map(_.toLong)

  def sizeString: Option[String] =
    size.map {
      case s if s < 1024               => s"$s B"
      case s if s < 1024 * 1024        => s"${s / 1024} KB"
      case s if s < 1024 * 1024 * 1024 => s"${s / (1024 * 1024)} MB"
      case s                           => s"${s / (1024 * 1024 * 1024)} GB"
    }

  def withBase(url: String): FileRef = copy(url = url + this.url)

object FileRef:
  import FileStore.Metadata

  private def metadata(
      fileType: Option[String] = None,
      size: Option[Long] = None
  ): Metadata =
    List(
      fileType.map(Metadata.FileType -> _),
      size.map(Metadata.Size -> _.toString)
    ).flatten.toMap

  def apply(
      name: String,
      url: String,
      fileType: Option[String] = None,
      size: Option[Long] = None
  ): Validated[FileRef] =
    for
      name <- Validated.nonEmptyString("file.name")(name)
      url <- Validated.nonEmptyString("file.url")(url)
    yield new FileRef(name, url, metadata(fileType, size))

  def unsafe(
      name: String,
      url: String,
      fileType: Option[String] = None,
      size: Option[Long] = None
  ): FileRef = new FileRef(name, url, metadata(fileType, size))

  def unsafe(name: String, url: String, metadata: Metadata) =
    new FileRef(name, url, metadata)
