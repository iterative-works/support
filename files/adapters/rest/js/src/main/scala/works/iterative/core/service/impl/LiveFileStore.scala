package works.iterative.core.service
package impl

import zio.*
import zio.json.*

import works.iterative.tapir.ClientEndpointFactory
import works.iterative.core.FileRef
import works.iterative.core.FileSupport
import works.iterative.tapir.CustomTapir
import sttp.model.Part
import sttp.model.MediaType
import works.iterative.tapir.endpoints.FileStoreEndpointsModule
import zio.stream.ZStream
import sttp.model.QueryParams

class LiveFileStore(
    factory: ClientEndpointFactory,
    endpoints: FileStoreEndpointsModule
) extends FileStoreWriter
    with CustomTapir:

    // Browsers have a problem with UTF-8 file names
    // So we encode them to hex on the client and back on the server.
    private def encodeFileName(name: String): String =
        BigInt(name.getBytes).toString(16)

    private val storeClient = factory.make(endpoints.store)

    private def metadataToPart(metadata: FileStore.Metadata): Part[Array[Byte]] =
        Part(
            "metadata",
            metadata.toJson.getBytes(),
            Some(MediaType.ApplicationJson),
            Some(encodeFileName("metadata.json")),
            Map.empty,
            Nil
        )

    override def store(
        files: List[FileSupport.FileRepr],
        metadata: FileStore.Metadata
    ): Op[List[FileRef]] =
        for
            parts <- ZIO
                .foreach(files)(_.toPart)
                .map(_.map(p => p.fileName.fold(p)(n => p.fileName(encodeFileName(n)))))
                .orDie
            refs <- storeClient(
                parts :+ metadataToPart(metadata)
            )
        yield refs

    override def store(
        name: String,
        file: Array[Byte],
        metadata: FileStore.Metadata
    ): UIO[FileRef] =
        storeClient(
            Seq(
                Part(
                    "file",
                    file,
                    metadata.get(FileStore.Metadata.FileType).flatMap(MediaType.parse(_).toOption),
                    Some(encodeFileName(name))
                )
            ) :+ metadataToPart(metadata)
        ).map(_.head)

    private val storeStreamClient = factory.make(endpoints.storeStream)

    override def store(
        name: String,
        content: ZStream[Any, Throwable, Byte],
        metadata: FileStore.Metadata
    ): UIO[FileRef] =
        storeStreamClient((
            QueryParams.fromMap(metadata),
            metadata.get(FileStore.Metadata.FileType).getOrElse("application/octet-stream"),
            content
        ))

    private val storeFileClient = factory.make(endpoints.storeFile)

    override def store(
        name: String,
        content: FileSupport.FileRepr,
        metadata: FileStore.Metadata
    ): UIO[FileRef] =
        storeFileClient((
            QueryParams.fromMap(metadata),
            metadata.get(FileStore.Metadata.FileType).getOrElse("application/octet-stream"),
            content
        ))

    private val updateClient = factory.make(endpoints.update)

    override def update(
        urls: List[String],
        metadata: FileStore.Metadata
    ): Op[Unit] =
        updateClient(endpoints.FileMetadataUpdate(urls, metadata))
end LiveFileStore

object LiveFileStore:
    def layer(
        endpoints: FileStoreEndpointsModule
    ): URLayer[ClientEndpointFactory, FileStoreWriter] =
        ZLayer {
            for factory <- ZIO.service[ClientEndpointFactory]
            yield LiveFileStore(factory, endpoints)
        }
end LiveFileStore
