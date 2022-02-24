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
import cz.e_bs.cmi.mdr.pdb.app.components.LinkSupport.*

object SeznamParametru:
  case class Parametr(
      id: String,
      nazev: String,
      popis: String,
      status: String,
      statusColor: Color,
      a: Anchor
  )
  type ViewModel = List[Parametr]

  private val parametrList = new StackedList[Parametr]

  def apply($m: Signal[ViewModel]): HtmlElement =
    div(
      cls := "bg-white shadow overflow-hidden sm:rounded-md",
      parametrList($m, _.id) { $i =>
        $i.map { i =>
          ListRow.ViewModel(
            title = i.nazev,
            topRight = RowTag.render(
              $i.map(x => RowTag.ViewModel(x.status, x.statusColor))
            ),
            bottomLeft = emptyNode,
            bottomRight = emptyNode,
            farRight = RowNext.render,
            containerElement = i.a
          )
        }
      }
    )
