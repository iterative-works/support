package works.iterative.core.service
package impl

import works.iterative.core.FileSupport.*
import works.iterative.core.FileRef
import zio.*

object InMemoryFileStoreWriter:
  val layer: ULayer[FileStoreWriter] = ZLayer.succeed {
    new FileStoreWriter:
      override def store(
          files: List[FileRepr],
          metadata: FileStore.Metadata
      ): Op[List[FileRef]] =
        ZIO.succeed(files.map(file => FileRef.unsafe(file.name, "#")))

      override def store(
          name: String,
          file: Array[Byte],
          contentType: Option[String],
          metadata: FileStore.Metadata
      ): Op[FileRef] =
        ZIO.succeed(FileRef.unsafe(name, "#"))

      override def update(
          urls: List[String],
          metadata: FileStore.Metadata
      ): Op[Unit] =
        ZIO.unit
  }
