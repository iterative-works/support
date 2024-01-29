package works.iterative.app

import zio.*

// Hacky way to manage languages for now, before we switch to something more useful
trait LanguageService:
    def currentLanguage: String
    def switchLanguage(language: String): Unit

object LanguageService:
    def static(language: String): LanguageService = new LanguageService:
        override def currentLanguage: String = language
        override def switchLanguage(lang: String) = ()

    def staticLayer(language: String): ZLayer[Any, Nothing, LanguageService] =
        ZLayer.succeed(static(language))

    def localStorage(appName: String): LanguageService = new LanguageService:
        override def switchLanguage(lang: String) =
            Option(org.scalajs.dom.window.localStorage)
                .foreach(_.setItem(s"${appName}_lang", lang))
            org.scalajs.dom.window.location.reload()
        override def currentLanguage: String =
            Option(org.scalajs.dom.window.localStorage)
                .flatMap(s => Option(s.getItem(s"${appName}_lang")))
                .getOrElse("cs")

    def localStorageLayer(appName: String): ZLayer[Any, Nothing, LanguageService] =
        ZLayer.succeed(localStorage(appName))

    val CS: LanguageService = static("cs")

    val CSLayer: ZLayer[Any, Nothing, LanguageService] = ZLayer.succeed(CS)
end LanguageService
