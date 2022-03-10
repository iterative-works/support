package mdr.pdb.app

import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js
import org.scalajs.dom
import com.raquo.laminar.api.L.{*, given}
import scala.scalajs.js.Date
import com.raquo.waypoint.Router
import com.raquo.waypoint.SplitRender
import mdr.pdb.app.services.DataFetcher
import scala.scalajs.js.JSON
import zio.*
import zio.json.*
import mdr.pdb.users.query.UserInfo
import mdr.pdb.app.state.AppState
import sttp.client3.*
import fiftyforms.tapir.{CustomTapir, BaseUri}
import mdr.pdb.users.query.client.UsersRepositoryLive
import mdr.pdb.proof.command.client.ProofCommandApiLive

@js.native
@JSImport("stylesheets/main.css", JSImport.Namespace)
object Css extends js.Any

@js.native
@JSImport("params/pdb-params.json", JSImport.Default)
object pdbParams extends js.Object

@JSExportTopLevel("app")
object Main extends ZIOApp:

  override type Environment = ZEnv & AppConfig & Router[Page] & AppState & Api &
    LaminarApp

  override val tag: EnvironmentTag[Environment] = EnvironmentTag[Environment]

  // TODO: config
  private val sttpLayer: ULayer[CustomTapir.Backend] = ZLayer.succeed(
    FetchBackend(
      FetchOptions(
        Some(dom.RequestCredentials.`same-origin`),
        Some(dom.RequestMode.`same-origin`)
      )
    )
  )

  private val baseUriLayer: TaskLayer[BaseUri] =
    AppConfig.layer.project(c => BaseUri(uri"${c.baseUrl}api/"))

  override val layer: ZLayer[ZIOAppArgs, Any, Environment] =
    ZEnv.live >+> AppConfig.layer >+> baseUriLayer >+> sttpLayer >+> Routes.router >+> UsersRepositoryLive.layer >+> ApiLive.layer >+> ProofCommandApiLive.layer >+> state.AppStateLive.layer >+> LaminarAppLive.layer

  override def run =
    for
      _ <- RIO.async[Environment, Unit](cb =>
        documentEvents.onDomContentLoaded
          .foreach(_ => cb(program))(unsafeWindowOwner)
      )
      // Keep running forever, otherwise the resources are released after the run finishes
      _ <- ZIO.never
    yield ()

  private def program: RIO[LaminarApp, Unit] =
    for _ <- LaminarApp.renderApp
    yield ()

  // Pull in the stylesheet
  val css: Css.type = Css
