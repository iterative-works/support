package works.iterative.autocomplete
package service
package impl

import zio.*

class InMemoryAutocompleteService(data: PartialFunction[String, List[AutocompleteEntry]])
    extends AutocompleteService:
    override def find(
        collection: String,
        q: String,
        limit: Int,
        language: String,
        context: Option[Map[String, String]]
    ): UIO[List[AutocompleteEntry]] =
        ZIO.succeed:
            data.lift(collection).getOrElse(Nil).filter(_.label.contains(q)).take(limit)

    override def load(
        collection: String,
        id: String,
        language: String
    ): UIO[Option[AutocompleteEntry]] =
        ZIO.succeed:
            data.lift(collection).getOrElse(Nil).find(_.value == id)
end InMemoryAutocompleteService

object InMemoryAutocompleteService:
    def simpleLayer(data: Map[String, List[String]]): ULayer[AutocompleteService] =
        ZLayer.succeed(
            new InMemoryAutocompleteService(
                data.map((k, v) =>
                    k -> v.map(v =>
                        AutocompleteEntry(
                            v,
                            v,
                            None,
                            Map.empty
                        )
                    )
                )
            )
        )

    def layer(data: Map[String, List[AutocompleteEntry]]): ULayer[AutocompleteService] =
        ZLayer.succeed(new InMemoryAutocompleteService(data))

    def constant(values: List[String]): AutocompleteService =
        val entries = values.map(v =>
            AutocompleteEntry(
                v,
                v,
                None,
                Map.empty
            )
        )
        new InMemoryAutocompleteService({ case _ => entries })
    end constant

    def constantLayer(values: List[String]): ULayer[AutocompleteService] =
        ZLayer.succeed(constant(values))
end InMemoryAutocompleteService
