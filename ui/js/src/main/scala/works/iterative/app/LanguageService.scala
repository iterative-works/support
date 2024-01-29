package works.iterative.app

// Hacky way to manage languages for now, before we switch to something more useful
trait LanguageService:
    def currentLanguage: String
    def switchLanguage(language: String): Unit
