package cz.e_bs.cmi.mdr.pdb.app.components

import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.Router
import cz.e_bs.cmi.mdr.pdb.app.Page

object PageLayout:
  case class ViewModel(
      navigation: NavigationBar.ViewModel,
      content: Option[HtmlElement]
  )
  def render(
      $m: Signal[ViewModel],
      mods: Modifier[HtmlElement]*
  )(using Router[Page]): HtmlElement =
    val $maybeContent = $m.map(_.content).split(_ => ())((_, c, _) => c)
    div(
      cls := "min-h-full",
      NavigationBar.render($m.map(_.navigation)),
      PageHeader.render,
      main(
        mods,
        child <-- $maybeContent.map(_.getOrElse(Loading))
      )
    )
