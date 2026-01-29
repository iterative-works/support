package works.iterative.core

trait CzechSupportPlatformSpecific:
    given czechOrdering: Ordering[String] =
        Ordering.comparatorToOrdering(using
            java.text.Collator
                .getInstance(java.util.Locale.forLanguageTag("cs-CZ"))
                .asInstanceOf[java.util.Comparator[String]]
        )
end CzechSupportPlatformSpecific
