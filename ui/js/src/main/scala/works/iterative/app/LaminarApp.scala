package works.iterative.app

import zio.*
import zio.json.*
import com.raquo.laminar.api.L.*
import com.raquo.waypoint.Router
import org.scalajs.dom
import com.raquo.waypoint.SplitRender
import works.iterative.core.MessageCatalogue
import works.iterative.ui.services.UserNotificationService
import works.iterative.app.PageRender

trait LaminarApp:
    def renderApp: Task[Unit]

object LaminarApp:
    def renderApp: RIO[LaminarApp, Unit] =
        ZIO.serviceWithZIO[LaminarApp](_.renderApp)

object LaminarAppLive:
    def layer[Env, P: Tag: JsonCodec](
        appShellFactory: AppShellFactory,
        connectors: Connectors[Env, P],
        home: P,
        notFound: P,
        pageTitle: P => String
    ): URLayer[
        Env & MessageCatalogue & AppExtensionService & UserNotificationService &
            AppConfig,
        LaminarApp
    ] =
        ZLayer {
            for
                appConfig <- ZIO.service[AppConfig]
                userNotificationService <- ZIO.service[UserNotificationService]
                extensionService <- ZIO.service[AppExtensionService]
                env <- ZIO.environment[Env]
                given MessageCatalogue <- ZIO.service[MessageCatalogue]
                connects = connectors.make
                given Router[P] = Routes[P](
                    appConfig.appUrl,
                    connects,
                    home,
                    notFound,
                    pageTitle
                ).router
            yield LaminarAppLive(
                appShellFactory,
                env,
                connects,
                extensionService
            ): LaminarApp
        }
end LaminarAppLive

class LaminarAppLive[Env, P](
    appShellFactory: AppShellFactory,
    env: ZEnvironment[Env],
    connectors: List[Connector[Env, P]],
    extensionService: AppExtensionService
)(using router: Router[P], messages: MessageCatalogue)
    extends LaminarApp:

    def renderApp: Task[Unit] = {
        for
            _ <- setupAirstream
            _ <- renderLaminar
        yield ()
    }.provideEnvironment(env)

    private def renderLaminar: RIO[Env, Unit] = {
        for
            renderP <- renderPage
            appContainer <- ZIO.attempt {
                dom.document.querySelector("#app")
            }
        yield render(appContainer, renderP)
    }.unit

    private def setupAirstream: Task[Unit] =
        ZIO.attempt {
            AirstreamError.registerUnhandledErrorCallback(
                AirstreamError.consoleErrorCallback
                    // AirstreamError.debuggerErrorCallback
            )
        }

    private val pageSplitter: RIO[Env, PageRender[P]] =
        ZIO.foldLeft(connectors)(
            SplitRender[P, HtmlElement](router.currentPageSignal)
        )((render, connector) => connector.connect(render))

    private def renderPage: RIO[Env, HtmlElement] =
        for
            splitter <- pageSplitter
            view <- extensionService.hookAppView(
                appShellFactory.make(splitter.signal).element
                    .amend(connectors.map(_.appMods))
            )
        yield view
end LaminarAppLive
