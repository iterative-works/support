package works.iterative
package autocomplete
package service
package impl.mariadb

import io.getquill.jdbczio.Quill
import io.getquill.*
import zio.*
import scala.annotation.unused

// TODO: how to abstract away all the details, leaving only the findQuery, loadQuery and toAutocompleteEntry?
// That would get us a complete generic implementation, with only queries to fill in.
// The simple approach of extracting a trait does not work, the run method complains about not being able to run the deferred inline method.
// I guess we would need to defer the code to the moment when the compiler has all the information it needs.
class GenericAutocompleteService(quill: Quill.Mysql[SnakeCase])
    extends AutocompleteService:
    import quill.*
    import GenericAutocompleteService.*

    inline protected def findQuery(
        collection: String,
        q: String,
        limit: Int,
        language: String,
        @unused context: Option[Map[String, String]]
    ): Query[Autocomplete] =
        query[Autocomplete]
            .filter(_.collection == lift(collection))
            .filter(_.label like lift(s"%$q%"))
            .filter(a => a.language.forall(_ == lift(language)))
            .filter(_.active == true)
            .sortBy(_.weight)(Ord.ascNullsLast)
            .take(lift(limit))
    end findQuery

    inline protected def loadQuery(
        collection: String,
        id: String,
        language: String,
        @unused context: Option[Map[String, String]]
    ): Query[Autocomplete] =
        query[Autocomplete]
            .filter(_.collection == lift(collection))
            .filter(_.value == lift(id))
            .filter(a => a.language.forall(_ == lift(language)))
            .take(1)
    end loadQuery

    protected def toAutocompleteEntry(a: Autocomplete): AutocompleteEntry =
        AutocompleteEntry(a.value, a.label, a.text)

    override final def find(
        collection: String,
        q: String,
        limit: Int,
        language: String,
        context: Option[Map[String, String]]
    ): UIO[List[AutocompleteEntry]] =
        val usedValues = context.map(_.values.toSet).getOrElse(Set.empty)
        run(findQuery(collection, q, limit, language, context)).map(
            _.filterNot(v => usedValues.contains(v.value)).map(toAutocompleteEntry)
        ).orDie
    end find

    override final def load(
        collection: String,
        id: String,
        lang: String,
        context: Option[Map[String, String]]
    ): UIO[Option[AutocompleteEntry]] =
        run(loadQuery(collection, id, lang, context)).map(
            _.headOption.map(toAutocompleteEntry)
        ).orDie
end GenericAutocompleteService

object GenericAutocompleteService:
    final case class Autocomplete(
        collection: String,
        language: Option[String],
        value: String,
        label: String,
        text: Option[String],
        weight: Option[Int],
        active: Boolean
    )

    val layer: URLayer[javax.sql.DataSource, AutocompleteService] = ZLayer {
        for ds <- ZIO.service[javax.sql.DataSource]
        yield GenericAutocompleteService(Quill.Mysql(SnakeCase, ds))
    }
end GenericAutocompleteService
