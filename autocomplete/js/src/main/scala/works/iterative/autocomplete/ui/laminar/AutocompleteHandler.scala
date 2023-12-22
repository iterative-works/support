package works.iterative.autocomplete
package ui
package laminar

import zio.Runtime
import com.raquo.laminar.api.L.*
import works.iterative.autocomplete.service.AutocompleteService
import works.iterative.ui.laminar.*

trait AutocompleteHandler:
    def find(q: String): EventStream[List[AutocompleteEntry]]
    def load(id: String): EventStream[Option[AutocompleteEntry]]

object AutocompleteHandler:
    def fromService(
        service: AutocompleteService,
        collection: String,
        limit: Int,
        language: String
    )(using Runtime[Any]): AutocompleteHandler =
        new AutocompleteHandler:
            override def find(q: String): EventStream[List[AutocompleteEntry]] =
                service.find(collection, q, limit, language).map(_.toList).toEventStream

            override def load(id: String): EventStream[Option[AutocompleteEntry]] =
                service.load(collection, id, language).toEventStream
end AutocompleteHandler
