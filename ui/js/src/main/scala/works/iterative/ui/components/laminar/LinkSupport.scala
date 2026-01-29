package works.iterative.ui.components.laminar

import com.raquo.laminar.api.L.*

object LinkSupport:

    extension [El <: org.scalajs.dom.EventTarget](
        ep: EventProcessor[org.scalajs.dom.MouseEvent, org.scalajs.dom.MouseEvent]
    )
        def noKeyMod =
            ep.filter(ev => !(ev.ctrlKey || ev.metaKey || ev.shiftKey || ev.altKey))
end LinkSupport
