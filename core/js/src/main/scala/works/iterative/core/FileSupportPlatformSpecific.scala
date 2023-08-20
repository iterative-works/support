package works.iterative.core

trait FileSupportPlatformSpecific:
  type FileRepr = org.scalajs.dom.File

  extension (f: FileRepr) def name: String = f.name
