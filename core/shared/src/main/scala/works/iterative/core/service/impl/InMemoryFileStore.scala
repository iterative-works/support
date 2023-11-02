package works.iterative.core.service
package impl

import works.iterative.core.FileSupport.*
import works.iterative.core.FileRef
import zio.*

object InMemoryFileStore:
  val layer: ULayer[FileStore] = ZLayer.succeed {
    new FileStore:
      override def store(files: List[FileRepr]): Op[List[FileRef]] =
        ZIO.succeed(files.map(file => FileRef.unsafe(file.name, "#")))

      override def store(
          name: String,
          file: Array[Byte],
          contentType: Option[String]
      ): Op[FileRef] =
        ZIO.succeed(FileRef.unsafe(name, "#"))

      override def load(url: String): Op[Option[Array[Byte]]] =
        ZIO.succeed(None)
  }
