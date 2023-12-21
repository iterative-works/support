package works.iterative.autocomplete

final case class AutocompleteEntry(
    value: String,
    label: String,
    text: Option[String] = None,
    data: Map[String, String] = Map.empty
)
