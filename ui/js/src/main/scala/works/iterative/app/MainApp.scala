package works.iterative.app

import org.scalajs.dom
import com.raquo.laminar.api.L.*
import zio.*
import works.iterative.ui.components.tailwind.TailwindSupport

trait MainApp extends ZIOAppDefault with TailwindSupport:
    protected def mainApp = runOnDomContentLoaded(
        works.iterative.app.LaminarApp.renderApp *> ZIO.never
    )

    protected def runOnDomContentLoaded[T](continue: RIO[T, Unit]) =
        ZIO.async[T, Throwable, Unit](cb =>
            if dom.document.readyState == dom.DocumentReadyState.loading then
                documentEvents(_.onDomContentLoaded).foreach(_ => cb(continue))(
                    unsafeWindowOwner
                )
                ()
            else cb(continue)
        )
end MainApp
