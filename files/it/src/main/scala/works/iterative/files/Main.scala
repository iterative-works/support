package works.iterative.files

import zio.*
import works.iterative.tapir.CustomTapir
import works.iterative.tapir.api.FileApi
import works.iterative.tapir.endpoints.FileStoreEndpointsModule
import works.iterative.server.http.HttpServer
import works.iterative.tapir.Http4sCustomTapir
import works.iterative.core.service.FileStoreWriter
import org.http4s.HttpRoutes
import works.iterative.server.http.impl.blaze.BlazeHttpServer
import works.iterative.core.FileRef
import works.iterative.core.service.FileStore.Metadata
import zio.stream.ZStream
import java.security.MessageDigest
import zio.nio.channels.AsynchronousFileChannel
import zio.nio.file.Path
import java.nio.file.StandardOpenOption

// A test HTTP4S server to check file uploads
object Main extends ZIOAppDefault:
    type Env = FileStoreWriter

    val api = new FileApi(FileStoreEndpointsModule(CustomTapir.endpoint)) {}

    val interpreter = new Http4sCustomTapir[Env] {}

    val routes: HttpRoutes[[A] =>> RIO[Env, A]] =
        interpreter.from(List(api.file.storeStream)).toRoutes

    val program =
        for
            server <- ZIO.service[HttpServer]
            _ <- server.serve[Env](_ => routes)
        yield ()

    override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] =
        program.provide(
            BlazeHttpServer.layer,
            ZLayer.succeed:
                new FileStoreWriter:
                    override def store(
                        name: String,
                        content: ZStream[Any, Throwable, Byte],
                        metadata: Metadata
                    ): UIO[FileRef] =
                        val md = MessageDigest.getInstance("SHA-256")
                        ZIO.scoped {
                            for
                                outChannel <- AsynchronousFileChannel.open(
                                    Path("test.out"),
                                    StandardOpenOption.CREATE,
                                    StandardOpenOption.WRITE
                                )
                                _ <- content.tap(ch => ZIO.attempt(md.update(ch))).run(
                                    outChannel.sink(0)
                                )
                                digest <- ZIO.attempt(md.digest())
                                ref <- ZIO.succeed(FileRef.unsafe(
                                    name,
                                    digest.map("%02X".format(_)).mkString.toLowerCase(),
                                    None,
                                    None
                                ))
                            yield ref
                        }.orDie
                    end store
                    override def update(urls: List[String], metadata: Metadata): UIO[Unit] = ???
        )
end Main
