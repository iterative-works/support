package works.iterative.autocomplete.ui.laminar

import zio.Runtime
import com.raquo.laminar.api.L.*
import works.iterative.autocomplete.AutocompleteEntry
import works.iterative.autocomplete.service.AutocompleteService
import works.iterative.core.Language
import works.iterative.ui.components.laminar.forms.FieldId
import works.iterative.ui.laminar.*

class ZIOAutocompleteRegistry(
    service: AutocompleteService,
    language: Language,
    mapping: Map[FieldId, ZIOAutocompleteRegistry.Config] = Map.empty
)(using Runtime[Any])
    extends AutocompleteRegistry:
    override def queryFor(id: FieldId): AutocompleteQuery = mapping.get(id) match
    case Some(c) => new AutocompleteQuery:
            override def find(q: String): EventStream[List[AutocompleteEntry]] =
                service.find(c.collection, q, c.limit, language.value).map(
                    _.toList
                ).toEventStream

            override def load(id: String): EventStream[Option[AutocompleteEntry]] =
                service.load(c.collection, id, language.value).toEventStream

    case None => AutocompleteQuery.empty

    def addMapping(id: FieldId, collection: String, limit: Int = 20): ZIOAutocompleteRegistry =
        ZIOAutocompleteRegistry(
            service,
            language,
            mapping + (id -> ZIOAutocompleteRegistry.Config(collection, limit))
        )
end ZIOAutocompleteRegistry

object ZIOAutocompleteRegistry:
    final case class Config(collection: String, limit: Int = 20)
