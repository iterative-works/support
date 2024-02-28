package works.iterative.autocomplete

final case class AutocompleteEntry(
    value: String,
    label: String,
    text: Option[String] = None,
    data: Map[String, String] = Map.empty
)

object AutocompleteEntry:
    def string(value: String): AutocompleteEntry = AutocompleteEntry(value, value, None, Map.empty)

    object keys:
        val href: String = "href"
