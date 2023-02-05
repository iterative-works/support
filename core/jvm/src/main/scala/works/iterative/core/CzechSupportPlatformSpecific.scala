package works.iterative.core

trait CzechSupportPlatformSpecific:
  given czechOrdering: Ordering[String] =
    Ordering.comparatorToOrdering(
      java.text.Collator
        .getInstance(java.util.Locale.forLanguageTag("cs-CZ"))
        .asInstanceOf[java.util.Comparator[String]]
    )
