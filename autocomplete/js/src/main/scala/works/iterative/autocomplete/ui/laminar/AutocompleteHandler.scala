package works.iterative.autocomplete
package ui
package laminar

import zio.Runtime
import com.raquo.laminar.api.L.*
import works.iterative.autocomplete.service.AutocompleteService
import works.iterative.ui.laminar.*
import works.iterative.core.Validated
import works.iterative.core.Language
import works.iterative.ui.components.laminar.forms.InputSchema

trait AutocompleteHandler[A] extends AutocompleteQuery with InputSchema[A]

object AutocompleteHandler:
    def fromService[A: InputSchema](
        collection: String,
        limit: Int = 20,
        language: Language = Language.CS
    )(service: AutocompleteService)(using
        Runtime[Any]
    ): AutocompleteHandler[A] =
        val codec = summon[InputSchema[A]]
        new AutocompleteHandler:
            override def find(q: String): EventStream[List[AutocompleteEntry]] =
                service.find(collection, q, limit, language.value).map(
                    _.toList
                ).toEventStream

            override def load(id: String): EventStream[Option[AutocompleteEntry]] =
                service.load(collection, id, language.value).toEventStream

            export codec.*
        end new
    end fromService

    def fromServiceR[A: InputSchema](
        collection: String,
        limit: Int = 20,
        language: Language = Language.CS
    )(using
        Runtime[AutocompleteService]
    ): AutocompleteHandler[A] =
        val codec = summon[InputSchema[A]]
        new AutocompleteHandler:
            override def find(q: String): EventStream[List[AutocompleteEntry]] =
                AutocompleteService.find(collection, q, limit, language.value).map(
                    _.toList
                ).toEventStream

            override def load(id: String): EventStream[Option[AutocompleteEntry]] =
                AutocompleteService.load(collection, id, language.value).toEventStream

            export codec.*
        end new
    end fromServiceR
end AutocompleteHandler
