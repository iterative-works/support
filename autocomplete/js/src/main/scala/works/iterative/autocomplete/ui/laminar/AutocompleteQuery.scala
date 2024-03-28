package works.iterative.autocomplete
package ui
package laminar

import com.raquo.laminar.api.L.*

trait AutocompleteQuery:
    def find(q: String): EventStream[List[AutocompleteEntry]]
    def load(id: String): EventStream[Option[AutocompleteEntry]]
    def withContextSignal(context: Signal[Option[Map[String, String]]]): AutocompleteQuery
end AutocompleteQuery

object AutocompleteQuery:
    val empty: AutocompleteQuery = new AutocompleteQuery:
        override def find(q: String): EventStream[List[AutocompleteEntry]] = EventStream.empty
        override def load(id: String): EventStream[Option[AutocompleteEntry]] = EventStream.empty
        override def withContextSignal(context: Signal[Option[Map[String, String]]])
            : AutocompleteQuery =
            empty
end AutocompleteQuery
