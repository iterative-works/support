package works.iterative.core.service

import works.iterative.core.FileSupport.*
import works.iterative.core.FileRef

import zio.*

trait FileStoreWriter:
    type Op[A] = UIO[A]

    def store(
        files: List[FileRepr],
        metadata: FileStore.Metadata
    ): Op[List[FileRef]]

    def store(
        name: String,
        file: Array[Byte],
        contentType: Option[String],
        metadata: FileStore.Metadata
    ): Op[FileRef]

    def update(urls: List[String], metadata: FileStore.Metadata): Op[Unit]
end FileStoreWriter

trait FileStoreLoader:
    type Op[A] = UIO[A]

    def load(url: String): Op[Option[Array[Byte]]]
end FileStoreLoader

trait FileStoreResolver:
    type Op[A] = UIO[A]

    def toAbsoluteURL(url: String): Op[String]
end FileStoreResolver

object FileStore:
    type Metadata = Map[String, String]

    object Metadata:
        val FileType = "fileType"
        val Size = "size"
        val Links = "links"
        val Kind = "kind"
        val UploadedBy = "uploadedBy"
    end Metadata

    def store(
        files: List[FileRepr],
        metadata: Metadata
    ): URIO[FileStoreWriter, List[FileRef]] =
        ZIO.serviceWithZIO(_.store(files, metadata))

    def store(
        name: String,
        file: Array[Byte],
        contentType: Option[String],
        metadata: Metadata
    ): URIO[FileStoreWriter, FileRef] =
        ZIO.serviceWithZIO(_.store(name, file, contentType, metadata))

    def load(url: String): URIO[FileStoreLoader, Option[Array[Byte]]] =
        ZIO.serviceWithZIO(_.load(url))

    def update(
        urls: List[String],
        metadata: Metadata
    ): URIO[FileStoreWriter, Unit] =
        ZIO.serviceWithZIO(_.update(urls, metadata))

    def toAbsoluteURL(url: String): URIO[FileStoreResolver, String] =
        ZIO.serviceWithZIO(_.toAbsoluteURL(url))
end FileStore
