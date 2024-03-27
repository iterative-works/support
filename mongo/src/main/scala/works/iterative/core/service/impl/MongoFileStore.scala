package works.iterative.core
package service
package impl

import org.mongodb.scala.MongoClient
import org.mongodb.scala.gridfs.GridFSBucket
import zio.*
import zio.json.*
import works.iterative.mongo.MongoJsonFileRepository
import works.iterative.core.auth.PermissionTarget
import works.iterative.core.auth.service.AuthenticationService
import works.iterative.core.service.FileStore.Metadata
import java.util.HexFormat
import java.security.MessageDigest
import org.mongodb.scala.model.Updates
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.model.Filters
import org.bson.types.ObjectId

class MongoFileStore(
    collection: MongoCollection[BsonDocument],
    bucket: GridFSBucket,
    authenticationService: AuthenticationService
) extends FileStoreWriter
    with FileStoreLoader:
    import MongoFileStore.*

    def filterToQuery(f: Filter) =
        import org.mongodb.scala.model.Filters.*
        f match
            case Filter(Some(id), _, _, _)   => equal("_id", id)
            case Filter(_, Some(link), _, _) =>
                // TODO: legacy ref to poptavka, remove after updating the legacy data
                val evc = PermissionTarget(link).map(_.toString()).getOrElse(link)
                or(equal("metadata.poptavka", evc), in("metadata.links", link))
            case Filter(_, _, Some(digest), _) =>
                equal(s"metadata.${FileStore.Metadata.SHA256Digest}", digest)
            case Filter(_, _, _, Some(digestExists)) =>
                exists(s"metadata.${FileStore.Metadata.SHA256Digest}", digestExists)
            case _ => empty
        end match
    end filterToQuery

    private def idToUrl(id: String, name: String): String = s"/$id/$name"
    private def urlToId(url: String): String =
        // take the first of the last two parts of the url
        url.split("/").takeRight(2).head

    private val repository =
        MongoJsonFileRepository[FileStore.Metadata, Filter](bucket, filterToQuery)

    private def withUploader(
        metadata: FileStore.Metadata
    ): UIO[FileStore.Metadata] =
        for cu <- authenticationService.currentUser
        yield cu match
            case Some(u) =>
                metadata + (FileStore.Metadata.UploadedBy -> u.subjectId.target
                    .toString())
            case _ => metadata

    // TODO: add metadata
    override def store(
        name: String,
        file: Array[Byte],
        contentType: Option[String],
        metadata: FileStore.Metadata
    ): UIO[FileRef] =
        val size = Some(file.length.longValue())
        for
            cu <- authenticationService.currentUser
            digest <- computeDigest(file)
            existing <-
                repository.matching(Filter(None, None, Some(digest), None)).map(_.headOption)
            ref <- existing match
                case Some(existingRef) => ZIO.succeed(FileRef.unsafe(
                        name,
                        idToUrl(existingRef.id, name),
                        contentType,
                        size
                    ))
                case _ =>
                    for
                        m <- withUploader(metadata)
                        ref <- repository
                            .put(name, file, m + (FileStore.Metadata.SHA256Digest -> digest))
                            .map(id => FileRef.unsafe(name, idToUrl(id, name), contentType, size))
                    yield ref
        yield ref
        end for
    end store

    override def store(
        files: List[FileSupport.FileRepr],
        metadata: FileStore.Metadata
    ): UIO[List[FileRef]] =
        import FileSupport.*
        ZIO.foreach(files)(file =>
            for
                bytes <- file.toStream.runCollect.orDie
                m <- withUploader(metadata)
                ref <- store(file.name, bytes.toArray, None, m)
            yield ref
        )
    end store

    // FIXME: implement update metadata
    override def update(urls: List[String], metadata: Metadata): UIO[Unit] =
        ZIO.unit

    // TODO: stream the content
    def load(url: String): UIO[Option[Array[Byte]]] =
        for
            id <- ZIO.attempt(urlToId(url)).orDie
            bytes <- repository.find(id)
        yield bytes

    def digestFiles: UIO[Long] =
        def updateDigest(id: String, digest: String): UIO[Unit] =
            ZIO.async(cb =>
                collection.updateOne(
                    Filters.eq("_id", ObjectId(id)),
                    Updates.set(s"metadata.${FileStore.Metadata.SHA256Digest}", digest)
                ).subscribe(_ => cb(ZIO.unit), t => cb(ZIO.die(t)))
            )

        for
            files <- repository.matching(Filter(None, None, None, Some(false)))
            result <- ZIO.foldLeft(files)(0L)((c, file) =>
                repository.find(file.id).flatMap {
                    case Some(bytes) =>
                        for
                            digest <- computeDigest(bytes)
                            _ <- updateDigest(file.id, digest)
                        yield c + 1
                    case _ =>
                        ZIO.succeed(c)
                }
            )
        yield result
        end for
    end digestFiles

    private def computeDigest(bytes: Array[Byte]): UIO[String] =
        ZIO.attempt {
            val md = MessageDigest.getInstance("SHA-256")
            md.update(bytes)
            val digest = md.digest()
            HexFormat.of().formatHex(digest)
        }.orDie

end MongoFileStore

object MongoFileStore:
    def make(config: MongoFileConfig): URIO[MongoClient & AuthenticationService, MongoFileStore] =
        for
            client <- ZIO.service[MongoClient]
            collection <-
                ZIO.attempt(
                    client.getDatabase(config.db).getCollection[BsonDocument](
                        s"${config.collection}.files"
                    )
                ).orDie
            bucket <- ZIO
                .attempt(
                    GridFSBucket(client.getDatabase(config.db), config.collection)
                )
                .orDie
            authenticationService <- ZIO.service[AuthenticationService]
        yield MongoFileStore(collection, bucket, authenticationService)

    def make: URIO[MongoFileConfig & MongoClient & AuthenticationService, MongoFileStore] =
        ZIO.service[MongoFileConfig].flatMap(make(_))

    def forConfig(config: MongoFileConfig): URLayer[
        MongoClient & AuthenticationService,
        FileStoreWriter & FileStoreLoader
    ] = ZLayer(make(config))

    val layer: URLayer[
        MongoClient & MongoFileConfig & AuthenticationService,
        FileStoreWriter & FileStoreLoader
    ] = ZLayer(make)

    case class Filter(
        id: Option[String],
        link: Option[String],
        digest: Option[String],
        digestExists: Option[Boolean]
    )
end MongoFileStore
