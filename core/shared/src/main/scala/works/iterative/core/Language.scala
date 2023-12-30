package works.iterative.core

opaque type Language = String

object Language extends ValidatedStringFactory[Language](identity):
    val CS: Language = "cs"
    val EN: Language = "en"
    val DE: Language = "de"

    def apply(value: String): Validated[Language] =
        Validated.nonEmptyString("language")(value)
end Language

case class LanguagePreference(inOrder: List[Language])

object LanguagePreference:
    given defaultLanguagePreference: LanguagePreference = LanguagePreference(
        List(Language.CS, Language.EN)
    )
end LanguagePreference
