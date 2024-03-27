package works.iterative.autocomplete
package service
package impl.rest

import works.iterative.autocomplete.endpoints.AutocompleteEndpoints
import works.iterative.tapir.CustomTapir.*

trait AutocompleteApi(endpoints: AutocompleteEndpoints):
    val find: ZServerEndpoint[AutocompleteService, Any] =
        endpoints.find.zServerLogic(AutocompleteService.find)

    val load: ZServerEndpoint[AutocompleteService, Any] =
        endpoints.load.zServerLogic((c, q, l, o) =>
            AutocompleteService.load(c, q, l, Some(o.toMap))
        )
end AutocompleteApi
