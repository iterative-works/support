package works.iterative.core

opaque type Localized[A] = Map[Language, A]

object Localized:

  // Make sure we can't construct empty Localized
  // optionality should be signaled explicitly
  def apply[A](
      firstValue: (Language, A),
      additionalValues: (Language, A)*
  ): Localized[A] =
    Map((firstValue +: additionalValues)*)

  extension [A](a: Localized[A])
    def get(using preference: LanguagePreference): A =
      // Get the first language that is in the preference list
      // or fallback to the first language in the map
      preference.inOrder
        .flatMap(a.get)
        .headOption
        .getOrElse(a.values.head)
