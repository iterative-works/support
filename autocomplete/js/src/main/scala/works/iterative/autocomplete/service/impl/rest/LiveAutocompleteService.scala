package works.iterative.autocomplete
package service
package impl.rest

import zio.*
import works.iterative.tapir.ClientEndpointFactory
import works.iterative.autocomplete.endpoints.AutocompleteEndpoints
import sttp.model.QueryParams

class LiveAutocompleteService(factory: ClientEndpointFactory, endpoints: AutocompleteEndpoints)
    extends AutocompleteService:
    private val findClient = factory.make(endpoints.find)

    override def find(
        collection: String,
        q: String,
        limit: Int,
        lang: String,
        context: Option[Map[String, String]]
    ): UIO[List[AutocompleteEntry]] =
        findClient(collection, q, limit, lang, context)

    private val loadClient = factory.make(endpoints.load)

    override def load(
        collection: String,
        id: String,
        lang: String,
        context: Option[Map[String, String]]
    ): UIO[Option[AutocompleteEntry]] =
        loadClient(collection, id, lang, QueryParams.fromMap(context.getOrElse(Map.empty)))
end LiveAutocompleteService

object LiveAutocompleteService:
    val layer: URLayer[ClientEndpointFactory & AutocompleteEndpoints, AutocompleteService] =
        ZLayer.derive[LiveAutocompleteService]
