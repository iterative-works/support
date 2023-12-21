package works.iterative.autocomplete
package service
package impl.rest

import works.iterative.autocomplete.endpoints.AutocompleteEndpoints
import works.iterative.tapir.CustomTapir.*

trait AutocompleteApi(endpoints: AutocompleteEndpoints):
    val find: ZServerEndpoint[AutocompleteService, Any] =
        endpoints.find.zServerLogic((collection, q, limit, lang) =>
            AutocompleteService.find(collection, q, limit, lang)
        )

    val load: ZServerEndpoint[AutocompleteService, Any] =
        endpoints.load.zServerLogic((collection, q, language) =>
            AutocompleteService.load(collection, q, language)
        )
end AutocompleteApi
