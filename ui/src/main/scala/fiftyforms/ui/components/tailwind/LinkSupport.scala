package fiftyforms.ui.components.tailwind

import com.raquo.laminar.api.L.{*, given}
import com.raquo.domtypes.jsdom.defs.events.TypedTargetMouseEvent

object LinkSupport:

  extension [El <: org.scalajs.dom.EventTarget](
      ep: EventProcessor[TypedTargetMouseEvent[El], TypedTargetMouseEvent[El]]
  )
    def noKeyMod =
      ep.filter(ev => !(ev.ctrlKey || ev.metaKey || ev.shiftKey || ev.altKey))
