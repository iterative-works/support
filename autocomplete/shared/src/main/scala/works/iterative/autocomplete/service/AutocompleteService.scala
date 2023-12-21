package works.iterative
package autocomplete
package service

import zio.*

trait AutocompleteService:
    def find(
        collection: String,
        q: String,
        limit: Int,
        language: String
    ): UIO[List[AutocompleteEntry]]

    def load(
        collection: String,
        id: String,
        language: String
    ): UIO[Option[AutocompleteEntry]]
end AutocompleteService

object AutocompleteService:
    def find(
        collection: String,
        q: String,
        limit: Int,
        language: String
    ): URIO[AutocompleteService, List[AutocompleteEntry]] =
        ZIO.serviceWithZIO(_.find(collection, q, limit, language))

    def load(
        collection: String,
        q: String,
        language: String
    ): URIO[AutocompleteService, Option[AutocompleteEntry]] =
        ZIO.serviceWithZIO(_.load(collection, q, language))
end AutocompleteService
