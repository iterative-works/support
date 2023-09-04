package works.iterative.core

opaque type Language = String

object Language:
  val CS: Language = "cs"
  val EN: Language = "en"
  val DE: Language = "de"

  extension (l: Language) def value: String = l

case class LanguagePreference(inOrder: List[Language])

object LanguagePreference:
  given defaultLanguagePreference: LanguagePreference = LanguagePreference(
    List(Language.CS, Language.EN)
  )
