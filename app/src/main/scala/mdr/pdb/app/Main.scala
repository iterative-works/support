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

  override type Environment = ZEnv & LaminarApp

  override val tag: EnvironmentTag[Environment] = EnvironmentTag[Environment]

  override val layer: ZLayer[ZIOAppArgs, Any, Environment] =
    ZEnv.live ++ (Routes.layer >>> LaminarAppLive.layer)

  override def run =
    for
      _ <- RIO.async[LaminarApp, Unit](cb =>
        documentEvents.onDomContentLoaded
          .foreach(_ => cb(program))(unsafeWindowOwner)
      )
    yield ()

  private def program: RIO[LaminarApp, Unit] =
    import Routes.given
    for
      _ <- testApi
      _ <- LaminarApp.renderApp
    yield ()

  private val testApi: Task[Unit] = Task.attempt {
    Api(Some("/mdr/pdb/api"))
      .alive(())
      .foreach(org.scalajs.dom.console.log(_))(using
        scala.concurrent.ExecutionContext.global
      )
  }

  // Pull in the stylesheet
  val css: Css.type = Css
