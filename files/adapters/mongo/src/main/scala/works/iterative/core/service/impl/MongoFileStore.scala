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
import zio.stream.ZStream

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
            case Filter(Some(id), _, _, _)   => equal("_id", ObjectId(id))
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

    override def store(
        name: String,
        file: Array[Byte],
        metadata: FileStore.Metadata
    ): UIO[FileRef] =
        computeDigest(file).flatMap: digest =>
            def withDigest(mt: Metadata) = mt + (FileStore.Metadata.SHA256Digest -> digest)

            val findByDigest =
                repository.matching(Filter.digest(digest)).map: found =>
                    found.headOption.map: ref =>
                        FileRef.unsafe(name, idToUrl(ref.id, name), withDigest(metadata))

            val storeNewFile =
                withUploader(metadata).flatMap: m =>
                    val mt = withDigest(m)
                    repository
                        .put(name, file, mt)
                        .map: id =>
                            FileRef.unsafe(name, idToUrl(id, name), mt)

            findByDigest.someOrElseZIO(storeNewFile)
    end store

    def findUrlsByLink(link: String): UIO[List[FileRef]] =
        repository.matching(Filter(link = Some(link))).map(_.map(f =>
            FileRef.unsafe(f.name, idToUrl(f.id, f.name), f.metadata)
        ))

    def removeByUrl(url: String): UIO[Unit] =
        for
            id <- ZIO.attempt(urlToId(url)).orDie
            _ <- repository.remove(id)
        yield ()

    // TODO: deduplication and file digest after
    override def store(
        name: String,
        content: ZStream[Any, Throwable, Byte],
        metadata: Metadata
    ): UIO[FileRef] =
        withUploader(metadata).flatMap: m =>
            repository
                .put(name, content, m)
                .map: id =>
                    FileRef.unsafe(name, idToUrl(id, name), m)
    end store

    override def store(
        name: String,
        content: FileSupport.FileRepr,
        metadata: Metadata
    ): UIO[FileRef] =
        withUploader(metadata).flatMap: m =>
            repository
                .put(name, content, m)
                .map: id =>
                    FileRef.unsafe(name, idToUrl(id, name), m)
    end store

    override def store(
        files: List[FileSupport.FileRepr],
        metadata: FileStore.Metadata
    ): UIO[List[FileRef]] =
        ZIO.foreach(files)(f => store(f.getName(), f, metadata))
    end store

    // FIXME: implement update metadata
    override def update(urls: List[String], metadata: Metadata): UIO[Unit] =
        ZIO.unit

    def load(url: String): UIO[Option[Array[Byte]]] =
        for
            id <- ZIO.attempt(urlToId(url)).orDie
            bytes <- repository.find(id)
        yield bytes

    def loadRef(url: String): UIO[Option[FileRef]] =
        for
            id <- ZIO.attempt(urlToId(url)).orDie
            ref <- repository.matching(Filter(id = Some(id)))
        yield ref.headOption.map: mf =>
            FileRef.unsafe(mf.name, url, mf.metadata)

    def loadStream(url: String): UIO[ZStream[Any, Throwable, Byte]] =
        for
            id <- ZIO.attempt(urlToId(url)).orDie
            stream <- repository.findStream(id)
        yield stream

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
    def make(config: MongoFileConfig): RIO[MongoClient & AuthenticationService, MongoFileStore] =
        for
            client <- ZIO.service[MongoClient]
            collection <-
                ZIO.attempt(
                    client.getDatabase(config.db).getCollection[BsonDocument](
                        s"${config.collection}.files"
                    )
                )
            bucket <- ZIO.attempt(GridFSBucket(client.getDatabase(config.db), config.collection))
            authenticationService <- ZIO.service[AuthenticationService]
        yield MongoFileStore(collection, bucket, authenticationService)

    def make: RIO[MongoClient & AuthenticationService, MongoFileStore] =
        ZIO.config(MongoFileConfig.config).flatMap(make(_))

    def forConfig(config: MongoFileConfig): RLayer[
        MongoClient & AuthenticationService,
        FileStoreWriter & FileStoreLoader
    ] = ZLayer(make(config))

    val layer: RLayer[
        MongoClient & AuthenticationService,
        FileStoreWriter & FileStoreLoader & MongoFileStore
    ] = ZLayer(make)

    case class Filter(
        id: Option[String] = None,
        link: Option[String] = None,
        digest: Option[String] = None,
        digestExists: Option[Boolean] = None
    )

    object Filter:
        def digest(digest: String): Filter = Filter(None, None, Some(digest), None)
end MongoFileStore
