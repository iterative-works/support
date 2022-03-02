package cz.e_bs.cmi.mdr.pdb.app.components

import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.Router
import cz.e_bs.cmi.mdr.pdb.app.Page
import cz.e_bs.cmi.mdr.pdb.app.Action
import fiftyforms.ui.components.Loading
import io.laminext.syntax.core.*

object PageLayout:
  case class ViewModel(
      navigation: NavigationBar.ViewModel,
      content: Option[HtmlElement]
  )
  def apply(actionBus: Observer[Action])(
      $m: Signal[ViewModel],
      mods: Modifier[HtmlElement]*
  )(using router: Router[Page]): HtmlElement =
    val $maybeContent = $m.map(_.content).split(_ => ())((_, c, _) => c)
    div(
      cls := "h-full flex flex-col",
      NavigationBar($m.map(_.navigation)),
      child.maybe <-- router.$currentPage.map(_.isRoot)
        .switch(None, Some(PageHeader(actionBus))),
      main(
        cls := "flex-grow-1 overflow-y-auto",
        mods,
        child <-- $maybeContent.map(_.getOrElse(Loading))
      )
    )
