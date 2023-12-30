package works.iterative.autocomplete.ui.laminar

import works.iterative.ui.components.laminar.forms.FieldId

trait AutocompleteRegistry:
    def queryFor(id: FieldId): AutocompleteQuery

object AutocompleteRegistry:
    val empty: AutocompleteRegistry = new AutocompleteRegistry:
        override def queryFor(id: FieldId): AutocompleteQuery = AutocompleteQuery.empty
