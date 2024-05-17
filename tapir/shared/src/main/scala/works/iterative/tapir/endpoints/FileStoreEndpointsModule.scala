package works.iterative.tapir.endpoints

import zio.json.*
import works.iterative.tapir.CustomTapir.*
import works.iterative.core.service.FileStore
import works.iterative.core.FileRef
import sttp.model.StatusCode
import works.iterative.tapir.codecs.Codecs.given
import sttp.model.Part
import sttp.tapir.CodecFormat
import sttp.capabilities.zio.ZioStreams
import zio.stream.ZStream
import sttp.tapir.Endpoint
import sttp.model.QueryParams

class FileStoreEndpointsModule(base: BaseEndpoint):
    final case class FileMetadataUpdate(
        urls: List[String],
        metadata: FileStore.Metadata
    ) derives JsonCodec, Schema

    /*
     * Store possibly multiple files as a multipart request.
     *
     * Metadata are expected as one of the parts call "metadata" as a JSON string dictionary, eg. Map[String, String].
     */
    val store: Endpoint[Unit, Seq[Part[Array[Byte]]], Unit, List[FileRef], Any] =
        base.post
            .in("file")
            .in(multipartBody.description(
                "Files to store. Metadata is expected as a part named 'metadata' in JSON format."
            ))
            .out(jsonBody[List[FileRef]])

    val storeStream: Endpoint[
        Unit,
        (QueryParams, String, ZStream[Any, Throwable, Byte]),
        Unit,
        FileRef,
        ZioStreams
    ] =
        base.post.in("file" / "upload").in(queryParams.description("File metadata")).in(
            header[String]("Content-Type")
        ).in(streamBinaryBody(
            ZioStreams
        )(CodecFormat.OctetStream())).out(jsonBody[FileRef])

    val storeFile: Endpoint[
        Unit,
        (QueryParams, String, TapirFile),
        Unit,
        FileRef,
        ZioStreams
    ] =
        base.post.in("file" / "upload").in(queryParams.description("File metadata")).in(
            header[String]("Content-Type")
        ).in(fileBody).out(jsonBody[FileRef])

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

    val loadStream
        : Endpoint[Unit, List[String], String, ZStream[Any, Throwable, Byte], ZioStreams] =
        base.get
            .in("file" / paths)
            .out(streamBinaryBody(ZioStreams)(CodecFormat.OctetStream()))
            .errorOut(statusCode(StatusCode.NotFound).and(stringBody))
end FileStoreEndpointsModule
