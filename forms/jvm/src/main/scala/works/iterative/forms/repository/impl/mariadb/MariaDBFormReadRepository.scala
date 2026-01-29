package portaly
package forms
package repository
package impl

import zio.*
import zio.json.*
import io.getquill.jdbczio.Quill
import io.getquill.*
import java.sql.Timestamp

class MariaDBFormReadRepository(quill: Quill.Mysql[io.getquill.SnakeCase])
    extends FormReadRepository:
    import quill.*
    import MariaDBFormReadRepository.{*, given}

    override def load(id: String, version: FormVersion): UIO[Option[Form]] =
        version match
            case FormVersion.Latest     => load(id)
            case FormVersion.Version(v) => load(id, v)

    private def load(id: String): UIO[Option[Form]] =
        run(
            quote(
                query[FormDescriptors]
                    .filter(_.id == lift(id))
                    .sortBy(_.updated)(Ord.desc)
                    .take(1)
            )
        ).map(_.headOption.map(_.descriptor)).orDie

    private def load(id: String, version: String): UIO[Option[Form]] =
        run(
            quote(
                query[FormDescriptors]
                    .filter(_.id == lift(id))
                    .filter(_.version == lift(version))
                    .take(1)
            )
        ).map(_.headOption.map(_.descriptor)).orDie
end MariaDBFormReadRepository

object MariaDBFormReadRepository:
    import portaly.forms.service.impl.rest.FormPersistenceCodecs.given
    given MappedEncoding[String, Form] = MappedEncoding(_.fromJson[Form].left.map(msg =>
        new RuntimeException(s"Error decoding form: $msg")
    ).fold(throw _, identity))
    given MappedEncoding[Form, String] = MappedEncoding(_.toJson)

    final case class FormDescriptors(
        id: String,
        version: String,
        descriptor: Form,
        updated: Timestamp
    )

    val layer: URLayer[javax.sql.DataSource, FormReadRepository] = ZLayer {
        for ds <- ZIO.service[javax.sql.DataSource]
        yield MariaDBFormReadRepository(Quill.Mysql(io.getquill.SnakeCase, ds))
    }
end MariaDBFormReadRepository
