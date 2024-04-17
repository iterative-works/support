package works.iterative
package autocomplete
package service

import zio.*

trait AutocompleteService:
    def find(
        collection: String,
        q: String,
        limit: Int,
        language: String,
        context: Option[Map[String, String]]
    ): UIO[List[AutocompleteEntry]]

    def load(
        collection: String,
        id: String,
        language: String,
        context: Option[Map[String, String]]
    ): UIO[Option[AutocompleteEntry]]
end AutocompleteService

object AutocompleteService:
    def find(
        collection: String,
        q: String,
        limit: Int,
        language: String,
        context: Option[Map[String, String]]
    ): URIO[AutocompleteService, List[AutocompleteEntry]] =
        ZIO.serviceWithZIO(_.find(collection, q, limit, language, context))

    def load(
        collection: String,
        q: String,
        language: String,
        context: Option[Map[String, String]]
    ): URIO[AutocompleteService, Option[AutocompleteEntry]] =
        ZIO.serviceWithZIO(_.load(collection, q, language, context))

    val empty: AutocompleteService =
        new AutocompleteService:
            def find(
                collection: String,
                q: String,
                limit: Int,
                language: String,
                context: Option[Map[String, String]]
            ): UIO[List[AutocompleteEntry]] = ZIO.succeed(Nil)

            def load(
                collection: String,
                id: String,
                language: String,
                context: Option[Map[String, String]]
            ): UIO[Option[AutocompleteEntry]] = ZIO.succeed(None)
end AutocompleteService
