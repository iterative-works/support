package works.iterative.core.service
package impl

import works.iterative.core.FileSupport.*
import works.iterative.core.FileRef
import zio.*

object InMemoryFileStore:
  val layer: ULayer[FileStore] = ZLayer.succeed {
    new FileStore:
      override def store(file: FileRepr): Op[FileRef] =
        for ref <- FileRef(file.name, "#").toZIO
        yield ref
  }
