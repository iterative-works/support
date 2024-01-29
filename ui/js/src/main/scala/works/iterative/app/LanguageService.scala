package works.iterative.app

// Hacky way to manage languages for now, before we switch to something more useful
trait LanguageService:
    def currentLanguage: String
    def switchLanguage(language: String): Unit

object LanguageService:
    def static(language: String): LanguageService = new LanguageService:
        override def currentLanguage: String = language
        override def switchLanguage(lang: String) = ()

    val CS: LanguageService = static("cs")
end LanguageService
