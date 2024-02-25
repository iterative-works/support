package works.iterative.autocomplete.ui.laminar

import works.iterative.ui.components.laminar.forms.FieldId

trait AutocompleteRegistry:
    def queryFor(id: FieldId): AutocompleteQuery =
        getQueryFor(id).getOrElse(AutocompleteQuery.empty)
    def getQueryFor(id: FieldId): Option[AutocompleteQuery]
    def withContext(ctx: Option[Map[String, String]]): AutocompleteRegistry
end AutocompleteRegistry

object AutocompleteRegistry:
    val empty: AutocompleteRegistry = new AutocompleteRegistry:
        override def getQueryFor(id: FieldId): Option[AutocompleteQuery] = None
        override def withContext(ctx: Option[Map[String, String]]): AutocompleteRegistry = this
end AutocompleteRegistry
