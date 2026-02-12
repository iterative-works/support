package works.iterative.tapir.endpoints

import works.iterative.tapir.CustomTapir.*
import sttp.model.QueryParams
import sttp.capabilities.fs2.Fs2Streams
import works.iterative.core.FileRef
import sttp.tapir.CodecFormat
import works.iterative.tapir.codecs.FileCodecs.given

trait FileStoreEndpointsModulePlatformSpecific(base: BaseEndpoint):
    self: FileStoreEndpointsModule =>

    /*
     * Provide direct FS2 support, as there is currently not a simple way to specify chunk size
     * for the ZIO streams using the ZIOStreams capability. Default is 16 bytes, and setting it
     * to a higher number, like 8192 that is used in http4s by default, really makes a difference.
     */
    val storeStreamFS2: Endpoint[
        Unit,
        (QueryParams, String, fs2.Stream[[A] =>> zio.RIO[Any, A], Byte]),
        Unit,
        FileRef,
        Fs2Streams[[A] =>> zio.RIO[Any, A]]
    ] =
        base.post.in("file" / "upload").in(queryParams.description("File metadata")).in(
            header[String]("Content-Type")
        ).in(streamBinaryBody(
            Fs2Streams[[A] =>> zio.RIO[Any, A]]
        )(CodecFormat.OctetStream())).out(jsonBody[FileRef])
end FileStoreEndpointsModulePlatformSpecific
