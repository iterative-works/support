package works.iterative.autocomplete.ui.laminar

import zio.*
import works.iterative.autocomplete.service.AutocompleteService
import works.iterative.ui.components.laminar.forms.InputSchema
import works.iterative.core.Language

trait AutocompleteProvider:
    def handlerFor[A: InputSchema](
        id: String,
        limit: Int = 20,
        language: Language = Language.CS
    ): AutocompleteHandler[A]
end AutocompleteProvider

class ZIOAutocompleteProvider(service: AutocompleteService)(using Runtime[Any])
    extends AutocompleteProvider:
    override def handlerFor[A: InputSchema](
        collection: String,
        limit: Int = 20,
        language: Language = Language.CS
    ): AutocompleteHandler[A] =
        AutocompleteHandler.fromService[A](collection, limit, language)(service)
end ZIOAutocompleteProvider

object ZIOAutocompleteProvider:
    val layer: URLayer[AutocompleteService, AutocompleteProvider] = ZLayer:
        for
            service <- ZIO.service[AutocompleteService]
            given Runtime[Any] <- ZIO.runtime[Any]
        yield ZIOAutocompleteProvider(service)
end ZIOAutocompleteProvider
