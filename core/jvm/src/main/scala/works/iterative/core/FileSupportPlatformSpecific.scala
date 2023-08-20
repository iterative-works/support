package works.iterative.core

trait FileSupportPlatformSpecific:
  type FileRepr = java.io.File

  extension (f: FileRepr) def name: String = f.getName
