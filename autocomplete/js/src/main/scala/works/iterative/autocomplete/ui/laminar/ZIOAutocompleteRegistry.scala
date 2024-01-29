package works.iterative.autocomplete.ui.laminar

import zio.*
import com.raquo.laminar.api.L.*
import works.iterative.autocomplete.AutocompleteEntry
import works.iterative.autocomplete.service.AutocompleteService
import works.iterative.ui.components.laminar.forms.FieldId
import works.iterative.ui.laminar.*
import works.iterative.app.LanguageService

class ZIOAutocompleteRegistry(
    service: AutocompleteService,
    // TODO: replace with iw-core LanguageService
    languageService: LanguageService,
    mapping: PartialFunction[FieldId, ZIOAutocompleteRegistry.Config]
)(using Runtime[Any]) extends AutocompleteRegistry:
    override def getQueryFor(id: FieldId): Option[AutocompleteQuery] = mapping.lift(id).map: c =>
        new AutocompleteQuery:
            override def find(q: String): EventStream[List[AutocompleteEntry]] =
                service.find(c.collection, q, c.limit, languageService.currentLanguage).map(
                    _.toList
                ).toEventStream

            override def load(id: String): EventStream[Option[AutocompleteEntry]] =
                service.load(c.collection, id, languageService.currentLanguage).map {
                    case None if !c.strict => Some(AutocompleteEntry(id, id, None, Map.empty))
                    case c                 => c
                }.toEventStream
end ZIOAutocompleteRegistry

object ZIOAutocompleteRegistry:
    final case class Config(collection: String, limit: Int = 20, strict: Boolean = false)

    def layer(mapping: PartialFunction[FieldId, Config])
        : URLayer[AutocompleteService & LanguageService, AutocompleteRegistry] =
        ZLayer {
            for
                service <- ZIO.service[AutocompleteService]
                languageService <- ZIO.service[LanguageService]
                given Runtime[Any] <- ZIO.runtime[Any]
            yield ZIOAutocompleteRegistry(service, languageService, mapping)
        }
end ZIOAutocompleteRegistry
