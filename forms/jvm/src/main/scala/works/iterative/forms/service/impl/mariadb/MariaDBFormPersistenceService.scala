package portaly.forms
package service
package impl
package mariadb

import zio.*
import io.getquill.*
import io.getquill.jdbczio.Quill
import io.github.arainko.ducktape.*
import java.time.Instant

class MariaDBFormPersistenceService(quill: Quill.Mysql[SnakeCase]) extends FormPersistenceService:
    import quill.*
    import MariaDBFormPersistenceService.*

    def put(key: FormIdent)(form: Option[SavedForm]): UIO[Unit] =
        form match
            case Some(f) =>
                val v = SavedFromRow.fromSavedForm(key.value, f)
                run(
                    quote(query[SavedFormRow].insertValue(lift(v)).onConflictUpdate(
                        _.formId -> _.formId,
                        _.formVersion -> _.formVersion,
                        _.encoding -> _.encoding,
                        _.data -> _.data,
                        _.timestamp -> _.timestamp
                    ))
                )
                    .unit.orDie
            case None =>
                run(
                    quote(query[SavedFormRow].filter(_.id == lift(key.value)).delete)
                ).unit.orDie
    end put

    def get(key: FormIdent): UIO[Option[SavedForm]] =
        run(
            quote(
                query[SavedFormRow]
                    .filter(_.id == lift(key.value))
                    .take(1)
            )
        ).map(_.headOption.map(SavedFromRow.toSavedForm)).orDie
end MariaDBFormPersistenceService

object MariaDBFormPersistenceService:
    final case class SavedFormRow(
        id: String,
        formId: String,
        formVersion: String,
        encoding: String,
        data: String,
        timestamp: Instant
    )

    object SavedFromRow:
        def fromSavedForm(id: String, savedForm: SavedForm): SavedFormRow =
            savedForm.into[SavedFormRow].transform(Field.const(_.id, id))
        def toSavedForm(savedFormRow: SavedFormRow): SavedForm =
            savedFormRow.to[SavedForm]
    end SavedFromRow

    val layer: URLayer[javax.sql.DataSource, FormPersistenceService] = ZLayer {
        for ds <- ZIO.service[javax.sql.DataSource]
        yield MariaDBFormPersistenceService(Quill.Mysql(SnakeCase, ds))
    }
end MariaDBFormPersistenceService
