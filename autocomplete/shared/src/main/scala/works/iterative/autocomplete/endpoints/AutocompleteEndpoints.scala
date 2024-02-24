package works.iterative
package autocomplete
package endpoints

import tapir.CustomTapir.*
import tapir.endpoints.BaseEndpoint
import codecs.Codecs.given

trait AutocompleteEndpoints(base: BaseEndpoint):
    val find: Endpoint[Unit, (String, String, Int, String, Option[Map[String, String]]), Unit, List[
        AutocompleteEntry
    ], Any] =
        base.get
            .in("autocomplete" / path[String]("collection") / "find")
            .in(query[String]("q"))
            .in(query[Int]("limit"))
            .in(query[String]("lang"))
            .in(jsonBody[Option[Map[String, String]]].description("Additional context"))
            .out(jsonBody[List[AutocompleteEntry]])

    val load: Endpoint[Unit, (String, String, String), Unit, Option[
        AutocompleteEntry
    ], Any] =
        base.get
            .in("autocomplete" / path[String]("collection") / "load")
            .in(query[String]("q"))
            .in(query[String]("lang"))
            .out(jsonBody[Option[AutocompleteEntry]])
end AutocompleteEndpoints
