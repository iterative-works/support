package works.iterative.core
package service
package impl

import org.mongodb.scala.MongoClient
import org.mongodb.scala.gridfs.GridFSBucket
import zio.*
import zio.json.*
import works.iterative.mongo.MongoJsonFileRepository
import FileSupport.*
import works.iterative.core.auth.PermissionTarget
import works.iterative.core.auth.service.AuthenticationService
import works.iterative.core.service.FileStore.Metadata

class MongoFileStore(
    bucket: GridFSBucket,
    authenticationService: AuthenticationService
) extends FileStoreWriter
    with FileStoreLoader:
  import MongoFileStore.*

  def filterToQuery(f: Filter) =
    import org.mongodb.scala.model.Filters.*
    f match {
      case Filter(Some(id), _)   => equal("_id", id)
      case Filter(_, Some(link)) =>
        // TODO: legacy ref to poptavka, remove after updating the legacy data
        val evc = PermissionTarget(link).map(_.toString()).getOrElse(link)
        or(equal("metadata.poptavka", evc), in("metadata.links", link))
      case _ => empty
    }

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
      m <- withUploader(metadata)
      ref <- repository
        .put(name, file, m)
        .map(id => FileRef.unsafe(name, idToUrl(id, name), contentType, size))
    yield ref

  override def store(
      files: List[FileSupport.FileRepr],
      metadata: FileStore.Metadata
  ): UIO[List[FileRef]] =
    ZIO.foreach(files)(file =>
      for
        bytes <- file.toStream.runCollect.orDie
        m <- withUploader(metadata)
        ref <- store(file.name, bytes.toArray, None, m)
      yield ref
    )

  // FIXME: implement update metadata
  override def update(urls: List[String], metadata: Metadata): UIO[Unit] =
    ZIO.unit

  // TODO: stream the content
  def load(url: String): UIO[Option[Array[Byte]]] =
    for
      id <- ZIO.attempt(urlToId(url)).orDie
      bytes <- repository.find(id)
    yield bytes

object MongoFileStore:
  val layer: URLayer[
    MongoClient & MongoFileConfig & AuthenticationService,
    FileStoreWriter & FileStoreLoader
  ] =
    ZLayer {
      for
        client <- ZIO.service[MongoClient]
        config <- ZIO.service[MongoFileConfig]
        bucket <- ZIO
          .attempt(
            GridFSBucket(client.getDatabase(config.db), config.collection)
          )
          .orDie
        authenticationService <- ZIO.service[AuthenticationService]
      yield MongoFileStore(bucket, authenticationService)
    }

  case class Filter private (
      id: Option[String],
      link: Option[String]
  )
