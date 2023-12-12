package works.iterative.server.http

import works.iterative.tapir.CustomTapir.*
import sttp.tapir.files.*
import sttp.tapir.Tapir
import sttp.tapir.EndpointInput

class SPAEndpoints[Env](config: SPAConfig):
    private val prefix: EndpointInput[Unit] = emptyInput
    /*
    config.prefix.toSeq
      .flatMap(_.split("/").toSeq)
      .foldLeft(emptyInput)((i, p) => i / p)
     */

    val serverEndpoints: List[ZServerEndpoint[Env, Any]] =
        config.filePath match
        case Some(filePath) =>
            List(
                staticFileGetServerEndpoint(prefix / config.appPath)(
                    s"${filePath}/${config.appIndex}"
                ),
                staticFilesGetServerEndpoint(prefix)(filePath)
            )
        case _ =>
            List(
                staticResourceGetServerEndpoint(prefix / config.appPath)(
                    classOf[Tapir].getClassLoader,
                    s"${config.resourcePath}/${config.appIndex}"
                ),
                staticResourcesGetServerEndpoint(prefix)(
                    classOf[Tapir].getClassLoader,
                    config.resourcePath
                )
            )
end SPAEndpoints
