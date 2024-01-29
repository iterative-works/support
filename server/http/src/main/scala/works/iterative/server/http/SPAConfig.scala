package works.iterative.server.http

import zio.*
import zio.config.*

/** Configuration for the single page application (SPA) endpoints.
  *
  * By default, the application will be served from prefix/app path, static resources from prefix/?
  * path.
  *
  * Under "app", only index.html will be served.
  *
  * As this has a catch-all route, it should be the last route in the list.
  */
final case class SPAConfig(
    /** The path under which the application HTML is always served. */
    appPath: String = "app",
    /** The filename for the app */
    appIndex: String = "index.html",
    /** Path to the files of the SPA application.
      *
      * If not set, the application will be served from the classpath under "app" package.
      */
    filePath: Option[String] = None,
    /** Path in the resources to get the files from.
      *
      * If filePath is set, this is ignored.
      */
    resourcePath: String = "app"
)

object SPAConfig:
    private def descriptor(name: String) =
        import ConfigDescriptor.*
        string("APPPATH").default("app") zip
            string("APPINDEX").default("index.html") zip
            string("FILEPATH").optional zip
            string("RESOURCEPATH").default(name)
    end descriptor

    val configDesc: ConfigDescriptor[SPAConfig] =
        import ConfigDescriptor.*
        nested("SPA")(descriptor("app")).to[SPAConfig]
    end configDesc

    def configDesc(prefix: String): ConfigDescriptor[SPAConfig] =
        import ConfigDescriptor.*
        nested("SPA")(nested(prefix.toUpperCase())(descriptor(prefix))).to[SPAConfig]

    val appFromEnv: ZLayer[Any, ReadError[String], SPAConfig] =
        ZConfig.fromSystemEnv(
            configDesc,
            keyDelimiter = Some('_'),
            valueDelimiter = Some(',')
        )

    def fromEnv(prefix: String): ZLayer[Any, ReadError[String], SPAConfig] =
        ZConfig.fromSystemEnv(
            configDesc(prefix),
            keyDelimiter = Some('_'),
            valueDelimiter = Some(',')
        )

    def loadFromEnv(prefix: String): ZIO[Any, ReadError[String], SPAConfig] =
        read(configDesc(prefix).from(ConfigSource.fromSystemEnv(
            keyDelimiter = Some('_'),
            valueDelimiter = Some(',')
        )))
    end loadFromEnv

    def loadAppFromEnv: ZIO[Any, ReadError[String], SPAConfig] =
        read(configDesc.from(ConfigSource.fromSystemEnv(
            keyDelimiter = Some('_'),
            valueDelimiter = Some(',')
        )))
    end loadAppFromEnv
end SPAConfig
