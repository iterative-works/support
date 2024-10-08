package works.iterative.core.service

import works.iterative.core.FileSupport.*
import works.iterative.core.FileRef

import zio.*
import zio.stream.ZStream
import works.iterative.core.FileSupport

trait FileStoreWriter:
    type Op[A] = UIO[A]

    def store(
        name: String,
        content: ZStream[Any, Throwable, Byte],
        metadata: FileStore.Metadata
    ): Op[FileRef]

    def store(
        name: String,
        content: FileRepr,
        metadata: FileStore.Metadata
    ): Op[FileRef] = store(name, content.toStream, metadata)

    def store(
        name: String,
        file: Array[Byte],
        metadata: FileStore.Metadata
    ): Op[FileRef] = store(name, ZStream.fromChunk(Chunk.fromArray(file)), metadata)

    def store(
        files: List[FileRepr],
        metadata: FileStore.Metadata
    ): Op[List[FileRef]] = ZIO.foreach(files)(f => store(f.name, f, metadata))

    def update(urls: List[String], metadata: FileStore.Metadata): Op[Unit]
end FileStoreWriter

trait FileStoreLoader:
    type Op[A] = UIO[A]

    def load(url: String): Op[Option[Array[Byte]]]

    def loadRef(url: String): Op[Option[FileRef]]

    def loadStream(url: String): Op[ZStream[Any, Throwable, Byte]]
end FileStoreLoader

trait FileStoreResolver:
    type Op[A] = UIO[A]

    def toAbsoluteURL(url: String): Op[String]
end FileStoreResolver

object FileStore:
    type Metadata = Map[String, String]

    object Metadata:
        val FileName = "filename"
        val FileType = "fileType"
        val Size = "size"
        val Links = "links"
        val Kind = "kind"
        val UploadedBy = "uploadedBy"
        val SHA256Digest = "sha256Digest"
    end Metadata

    def store(
        files: List[FileRepr],
        metadata: Metadata
    ): URIO[FileStoreWriter, List[FileRef]] =
        ZIO.serviceWithZIO(_.store(files, metadata))

    def store(
        name: String,
        content: ZStream[Any, Throwable, Byte],
        metadata: Metadata
    ): URIO[FileStoreWriter, FileRef] =
        ZIO.serviceWithZIO(_.store(name, content, metadata))

    def store(
        name: String,
        content: FileSupport.FileRepr,
        metadata: Metadata
    ): URIO[FileStoreWriter, FileRef] =
        ZIO.serviceWithZIO(_.store(name, content, metadata))

    def store(
        name: String,
        file: Array[Byte],
        metadata: Metadata
    ): URIO[FileStoreWriter, FileRef] =
        ZIO.serviceWithZIO(_.store(name, file, metadata))

    def load(url: String): URIO[FileStoreLoader, Option[Array[Byte]]] =
        ZIO.serviceWithZIO(_.load(url))

    def loadStream(url: String): URIO[FileStoreLoader, ZStream[Any, Throwable, Byte]] =
        ZIO.serviceWithZIO(_.loadStream(url))

    def loadRef(url: String): URIO[FileStoreLoader, Option[FileRef]] =
        ZIO.serviceWithZIO(_.loadRef(url))

    def update(
        urls: List[String],
        metadata: Metadata
    ): URIO[FileStoreWriter, Unit] =
        ZIO.serviceWithZIO(_.update(urls, metadata))

    def toAbsoluteURL(url: String): URIO[FileStoreResolver, String] =
        ZIO.serviceWithZIO(_.toAbsoluteURL(url))
end FileStore
