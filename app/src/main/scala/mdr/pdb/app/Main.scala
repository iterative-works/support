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
import mdr.pdb.UserInfo
import mdr.pdb.app.state.AppState

@js.native
@JSImport("stylesheets/main.css", JSImport.Namespace)
object Css extends js.Any

@js.native
@JSImport("data/users.json", JSImport.Default)
object mockUsers extends js.Object

@js.native
@JSImport("params/pdb-params.json", JSImport.Default)
object pdbParams extends js.Object

@JSExportTopLevel("app")
object Main extends ZIOApp:

  override type Environment = ZEnv & AppConfig & Router[Page] & AppState & Api &
    LaminarApp

  override val tag: EnvironmentTag[Environment] = EnvironmentTag[Environment]

  // TODO: config
  override val layer: ZLayer[ZIOAppArgs, Any, Environment] =
    ZEnv.live >+> AppConfig.layer >+> Routes.router >+> ApiLive.layer >+> state.AppStateLive.layer >+> LaminarAppLive.layer

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
