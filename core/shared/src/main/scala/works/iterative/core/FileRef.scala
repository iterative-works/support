package works.iterative.core

/** Represents a reference to a file */
case class FileRef(
    name: String,
    url: String,
    fileType: Option[String],
    size: Option[String]
)
