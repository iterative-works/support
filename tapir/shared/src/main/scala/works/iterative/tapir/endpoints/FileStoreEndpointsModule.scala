package works.iterative.tapir.endpoints

import zio.json.*
import works.iterative.tapir.CustomTapir.*
import works.iterative.core.service.FileStore
import works.iterative.core.FileRef
import sttp.model.StatusCode
import works.iterative.tapir.codecs.Codecs.given
import sttp.model.Part

class FileStoreEndpointsModule(base: BaseEndpoint):
  final case class FileMetadataUpdate(
      urls: List[String],
      metadata: FileStore.Metadata
  ) derives JsonCodec,
        Schema

  val store: Endpoint[Unit, Seq[Part[Array[Byte]]], Unit, List[FileRef], Any] =
    base.post
      .in("file")
      .in(multipartBody)
      .out(jsonBody[List[FileRef]])

  val update: Endpoint[Unit, FileMetadataUpdate, Unit, Unit, Any] =
    base.patch
      .in("file")
      .in(jsonBody[FileMetadataUpdate])
      .out(statusCode(StatusCode.Accepted))

  val load: Endpoint[Unit, List[String], String, Array[Byte], Any] =
    base.get
      .in("file" / paths)
      .out(byteArrayBody)
      .errorOut(statusCode(StatusCode.NotFound).and(stringBody))
