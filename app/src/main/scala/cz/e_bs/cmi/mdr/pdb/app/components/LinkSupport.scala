package cz.e_bs.cmi.mdr.pdb.app.components

import com.raquo.laminar.api.L.{*, given}
import com.raquo.domtypes.jsdom.defs.events.TypedTargetMouseEvent

object LinkSupport:

  extension [El <: org.scalajs.dom.EventTarget](
      ep: EventProcessor[TypedTargetMouseEvent[El], TypedTargetMouseEvent[El]]
  )
    def noKeyMod =
      ep.filter(ev => !(ev.ctrlKey || ev.metaKey || ev.shiftKey || ev.altKey))
