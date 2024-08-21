package portaly.forms
package service
package impl
package browser

import zio.*
import zio.json.*
import org.scalajs.dom.window.sessionStorage

class SessionStoreFormPersistenceService extends FormPersistenceService:
    import SessionStoreFormPersistenceService.given

    override def put(id: FormIdent)(form: Option[SavedForm]): UIO[Unit] =
        if form.isEmpty then ZIO.attempt(sessionStorage.removeItem(id.value)).ignoreLogged
        else ZIO.attempt(sessionStorage.setItem(id.value, form.toJson)).ignoreLogged

    override def get(id: FormIdent): UIO[Option[SavedForm]] =
        for
            savedForm <- ZIO.attempt(Option(sessionStorage.getItem(id.value))).orDie
            decoded <- savedForm match
                case None => ZIO.none
                case Some(sf) => sf.fromJson[SavedForm] match
                        case Left(msg)   => ZIO.logWarning(msg) *> ZIO.none
                        case Right(data) => ZIO.some(data)
        yield decoded
end SessionStoreFormPersistenceService

object SessionStoreFormPersistenceService:
    val layer: URLayer[Any, FormPersistenceService] =
        ZLayer.succeed(SessionStoreFormPersistenceService())

    given JsonCodec[SavedForm] = DeriveJsonCodec.gen[SavedForm]
end SessionStoreFormPersistenceService
