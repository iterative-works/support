package cz.e_bs.cmi.mdr.pdb.app.pages.directory.components

import com.raquo.laminar.api.L.{*, given}
import cz.e_bs.cmi.mdr.pdb.UserInfo
import com.raquo.airstream.core.Signal
import com.raquo.waypoint.Router
import cz.e_bs.cmi.mdr.pdb.app.components.Avatar
import cz.e_bs.cmi.mdr.pdb.app.Page
import cz.e_bs.cmi.mdr.pdb.waypoint.components.Navigator

object UserRow:
  type ViewModel = UserInfo

  sealed trait Action
  case class Selected(value: ViewModel) extends Action

  def render($m: Signal[ViewModel])(using router: Router[Page]): HtmlElement =
    inline def avatarImage =
      Avatar($m.map(_.img)).avatarImage(10)

    li(
      div(
        cls := "relative px-6 py-5 flex items-center space-x-3 hover:bg-gray-50 focus-within:ring-2 focus-within:ring-inset focus-within:ring-pink-500",
        div(
          cls := "flex-shrink-0",
          child <-- avatarImage
        ),
        div(
          cls := "flex-1 min-w-0",
          child <-- $m.map { o =>
            a(
              Navigator.navigateTo[Page](Page.Detail(o.personalNumber)),
              cls := "focus:outline-none",
              span(
                cls := "absolute inset-0",
                aria.hidden := true
              ),
              p(
                cls := "text-sm font-medium text-gray-900",
                o.name
              ),
              p(
                cls := "text-sm text-gray-500 truncate",
                o.mainFunction
              )
            )
          }
        )
      )
    )
