package works.iterative.server.http

import scalatags.Text
import zio.*
import zio.json.*
import works.iterative.tapir.BaseUri

trait ViteSupport[T]:
    def preambleFor(entrypoint: String): T

class ScalatagsViteSupport(
    entrypoints: PartialFunction[String, ScalatagsViteSupport.Entries]
) extends ViteSupport[Seq[Text.Modifier]]:
    def mainCss: Seq[Text.Modifier] =
        if entrypoints.isDefinedAt("main.css") then entrypoints("main.css").stylesheets else Nil

    override def preambleFor(entrypoint: String): Seq[Text.Modifier] =
        if entrypoints.isDefinedAt(entrypoint) then
            entrypoints(entrypoint).preamble
        else Nil
end ScalatagsViteSupport

object ScalatagsViteSupport:
    final case class ViteConfig(
        viteBase: Option[String],
        viteFilePath: Option[String],
        viteResourcePath: Option[String]
    )

    final case class Entries(
        stylesheet: List[String] = Nil,
        module: List[String] = Nil,
        preload: List[String] = Nil
    ):
        import scalatags.Text.all.*
        val stylesheets: Seq[Text.Modifier] =
            stylesheet.map(v => link(rel := "stylesheet", href := v))
        val modules: Seq[Text.Modifier] = module.map(v => script(tpe := "module", src := v))
        val preloads: Seq[Text.Modifier] = preload.map(v => link(rel := "modulepreload", href := v))

        val preamble: Seq[Text.Modifier] = stylesheets ++ modules ++ preloads

        def withBaseUri(baseUri: BaseUri): Entries =
            def prepend(v: String) =
                if v.startsWith("/") || v.startsWith("http") then v
                else baseUri.href + "/" + v

            copy(
                stylesheet = stylesheet.map(prepend),
                module = module.map(prepend),
                preload = preload.map(prepend)
            )
        end withBaseUri
    end Entries

    val config: Config[ViteConfig] =
        import Config.*
        (string("base").optional ++ string("file").optional ++ string(
            "resource"
        ).optional).nested("vite").map(ViteConfig.apply)
    end config

    final case class ViteChunk(
        file: String,
        @jsonField("css") _css: Option[List[String]] = None,
        @jsonField("imports") _imports: Option[List[String]] = None,
        @jsonField("isEntry") _isEntry: Option[Boolean] = None
    ) derives JsonDecoder:
        val isEntry: Boolean = _isEntry.getOrElse(false)
        val css: List[String] = _css.getOrElse(Nil)
        val imports: List[String] = _imports.getOrElse(Nil)
    end ViteChunk

    val layer: Layer[Config.Error, ScalatagsViteSupport] =
        def devEntries(base: String): IO[Config.Error, PartialFunction[String, Entries]] =
            ZIO.succeed {
                case entry => Entries(module = List(s"${base}@vite/client", s"${base}$entry"))
            }

        def processManifest(
            manifest: Map[String, ViteChunk],
            baseUri: BaseUri
        ): Map[String, Entries] =
            def allCss(chunk: ViteChunk): List[String] =
                chunk.css ++ chunk.imports.flatMap(manifest.get).flatMap(allCss)

            def allPreloads(chunk: ViteChunk): List[String] =
                chunk.imports.flatMap(manifest.get).flatMap: ch =>
                    ch.file :: allPreloads(ch)

            def collectEntries(chunk: ViteChunk): Entries =
                Entries(
                    stylesheet = allCss(chunk),
                    module = List(chunk.file),
                    preload = allPreloads(chunk)
                ).withBaseUri(baseUri)

            manifest.keys.flatMap(e => manifest.get(e).map(e -> _)).map((k, v) =>
                k -> collectEntries(v)
            ).toMap
        end processManifest

        def loadManifest(
            load: ZIO[Scope, Throwable, ZInputStream],
            baseUri: BaseUri
        ): ZIO[Scope, Config.Error, Map[String, Entries]] =
            for
                is <- load.mapError(e =>
                    Config.Error.InvalidData(message = s"Cannot load manifest: $e")
                )
                raw <- is.readAll(4096).mapError(e =>
                    Config.Error.InvalidData(message = s"Cannot read manifest: $e")
                )
                manifest <-
                    ZIO.fromEither(raw.asString.fromJson[Map[String, ViteChunk]]).mapError(e =>
                        Config.Error.InvalidData(message = s"Cannot decode manifest: $e")
                    )
            yield processManifest(manifest, baseUri)

        def fileManifest(
            path: String,
            baseUri: BaseUri
        ): ZIO[Scope, Config.Error, Map[String, Entries]] =
            loadManifest(ZIO.readFileInputStream(path), baseUri)

        def resourceManifest(
            path: String,
            baseUri: BaseUri
        ): ZIO[Scope, Config.Error, Map[String, Entries]] =
            loadManifest(ZIO.readURLInputStream(getClass().getResource(path)), baseUri)

        def entriesFor(
            config: ViteConfig,
            baseUri: BaseUri
        ): ZIO[Scope, Config.Error, PartialFunction[String, Entries]] =
            config match
                case ViteConfig(Some(base), _, _) => devEntries(base)
                case ViteConfig(_, Some(path), _) => fileManifest(path, baseUri)
                case ViteConfig(_, _, Some(path)) => resourceManifest(path, baseUri)
                case _ => ZIO.fail(Config.Error.MissingData(message =
                        s"Missing manifest info, set either VITE_BASE, VITE_FILE or VITE_RESOURCE"
                    ))

        ZLayer.scoped {
            for
                config <- ZIO.config(config)
                baseUri <- ZIO.config(BaseUri.config)
                entries <- entriesFor(config, baseUri)
                _ <- ZIO.log(s"Vite support enabled, entries:\n${entries}")
            yield ScalatagsViteSupport(entries)
        }
    end layer
end ScalatagsViteSupport
