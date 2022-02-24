package cz.e_bs.cmi.mdr.pdb.app.components

import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.Router
import cz.e_bs.cmi.mdr.pdb.app.Page
import cz.e_bs.cmi.mdr.pdb.app.Action

object PageLayout:
  case class ViewModel(
      navigation: NavigationBar.ViewModel,
      content: Option[HtmlElement]
  )
  def apply(actionBus: Observer[Action])(
      $m: Signal[ViewModel],
      mods: Modifier[HtmlElement]*
  )(using Router[Page]): HtmlElement =
    val $maybeContent = $m.map(_.content).split(_ => ())((_, c, _) => c)
    div(
      cls := "min-h-full",
      NavigationBar($m.map(_.navigation)),
      PageHeader(actionBus),
      main(
        mods,
        child <-- $maybeContent.map(_.getOrElse(Loading))
      )
    )
