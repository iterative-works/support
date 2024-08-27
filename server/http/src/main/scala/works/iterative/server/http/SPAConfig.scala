package works.iterative.server.http

import zio.*

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
    def appConfig(name: String) =
        import Config.*
        string("apppath").withDefault("app") ++
            string("appindex").withDefault("index.html") ++
            string("filepath").optional ++
            string("resourcepath").withDefault(name)
    end appConfig

    val config: Config[SPAConfig] =
        appConfig("app").nested("spa").map(SPAConfig.apply)
    end config

    def config(prefix: String): Config[SPAConfig] =
        appConfig(prefix).nested(prefix).nested("spa").map(SPAConfig.apply)
end SPAConfig
