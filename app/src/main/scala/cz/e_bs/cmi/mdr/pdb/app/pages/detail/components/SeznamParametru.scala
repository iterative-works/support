package cz.e_bs.cmi.mdr.pdb.app.pages.detail.components

import com.raquo.laminar.api.L.{*, given}
import cz.e_bs.cmi.mdr.pdb.app.components.list.{
  StackedList,
  ListRow,
  RowTag,
  PropList,
  IconText,
  RowNext
}
import cz.e_bs.cmi.mdr.pdb.app.components.Color
import cz.e_bs.cmi.mdr.pdb.waypoint.components.Navigator
import com.raquo.waypoint.Router
import cz.e_bs.cmi.mdr.pdb.app.Page

object SeznamParametru:
  sealed trait Action
  case object Selected extends Action

  case class Parametr(
      id: String,
      nazev: String,
      status: String,
      statusColor: Color
  )
  type ViewModel = List[Parametr]

  private val parametrList = new StackedList[Parametr]

  def render($m: Signal[ViewModel])(pageF: Parametr => Page)(using
      router: Router[Page]
  ): HtmlElement =
    div(
      cls := "bg-white shadow overflow-hidden sm:rounded-md",
      parametrList.render($m, _.id) { $i =>
        $i.map { i =>
          ListRow.ViewModel(
            title = i.nazev,
            topRight = RowTag.render(
              $i.map(x => RowTag.ViewModel(x.status, x.statusColor))
            ),
            bottomLeft = emptyNode,
            bottomRight = emptyNode,
            farRight = RowNext.render,
            containerElement = a(Navigator.navigateTo(pageF(i)))
          )
        }
      }
    )
