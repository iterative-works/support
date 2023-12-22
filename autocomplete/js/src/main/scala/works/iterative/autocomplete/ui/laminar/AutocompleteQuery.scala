package works.iterative.autocomplete
package ui
package laminar

import com.raquo.laminar.api.L.*

trait AutocompleteQuery:
    def find(q: String): EventStream[List[AutocompleteEntry]]
    def load(id: String): EventStream[Option[AutocompleteEntry]]
