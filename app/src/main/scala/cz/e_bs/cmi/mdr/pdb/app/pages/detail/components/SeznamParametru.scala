package cz.e_bs.cmi.mdr.pdb.app.pages.detail.components

import com.raquo.laminar.api.L.{*, given}
import fiftyforms.ui.components.tailwind.list.{
  StackedList,
  ListRow,
  RowTag,
  PropList,
  IconText,
  RowNext
}
import fiftyforms.ui.components.tailwind.Color
import fiftyforms.ui.components.tailwind.LinkSupport.*

object SeznamParametru:
  type ViewModel = List[DetailParametru.ViewModel]

  private val parametrList = new StackedList[DetailParametru.ViewModel]

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
