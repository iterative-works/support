package portaly.forms
package impl

import zio.*
import zio.json.*
import com.raquo.laminar.api.L.*
import works.iterative.ui.laminar.*
import service.{FormPersistenceService, SavedForm}

enum PersistenceData:
    case Draft(data: FormR)
    case Done(data: FormR)
    case Reset
end PersistenceData

trait PersistenceProvider:
    def writer(id: FormIdent, formId: String, formVersion: String): Observer[PersistenceData]
    def reader(id: FormIdent, formId: String, formVersion: String): EventStream[Option[FormR]]

object PersistenceProvider:
    val empty: PersistenceProvider =
        new PersistenceProvider:
            override def writer(
                id: FormIdent,
                formId: String,
                formVersion: String
            ): Observer[PersistenceData] =
                Observer.empty

            override def reader(
                id: FormIdent,
                formId: String,
                formVersion: String
            ): EventStream[Option[FormR]] =
                EventStream.empty
end PersistenceProvider

class ZIOPersistenceProvider(
    draftService: FormPersistenceService,
    finalService: FormPersistenceService,
    relaxedVersions: Boolean
)(using Runtime[Any]) extends PersistenceProvider:
    def this(draftService: FormPersistenceService, relaxedVersions: Boolean)(using Runtime[Any]) =
        this(draftService, FormPersistenceService.empty, relaxedVersions)
    def this(draftService: FormPersistenceService)(using Runtime[Any]) = this(draftService, true)
    def this(draftService: FormPersistenceService, finalService: FormPersistenceService)(using
        Runtime[Any]
    ) =
        this(draftService, finalService, true)

    override def writer(
        id: FormIdent,
        formId: String,
        formVersion: String
    ): Observer[PersistenceData] =
        Observer.fromZIO(data =>
            for
                now <- Clock.instant
                _ <- data match
                    case PersistenceData.Draft(data) =>
                        draftService.put(id)(Some(SavedForm(
                            formId,
                            formVersion,
                            FormR.jsonMediaType,
                            data.toJson,
                            now
                        )))
                    case PersistenceData.Done(data) =>
                        finalService.put(id)(Some(SavedForm(
                            formId,
                            formVersion,
                            FormR.jsonMediaType,
                            data.toJson,
                            now
                        ))) *> draftService.put(id)(None)
                    case PersistenceData.Reset => draftService.put(id)(None)
            yield ()
        )

    override def reader(
        id: FormIdent,
        formId: String,
        formVersion: String
    ): EventStream[Option[FormR]] =
        EventStream.fromZIO:
            for
                savedForm <- draftService.get(id)
                data <- savedForm match
                    case Some(sf)
                        if formId == sf.formId && (relaxedVersions || formVersion == sf.formVersion) =>
                        sf.data.fromJson[FormR] match
                            case Left(msg)   => ZIO.logWarning(msg) *> ZIO.none
                            case Right(data) => ZIO.some(data)
                    case _ => ZIO.none
            yield data
end ZIOPersistenceProvider
