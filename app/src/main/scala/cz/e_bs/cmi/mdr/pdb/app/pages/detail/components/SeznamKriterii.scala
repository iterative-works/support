package cz.e_bs.cmi.mdr.pdb.app.pages.detail.components

import com.raquo.laminar.api.L.{*, given}
import cz.e_bs.cmi.mdr.pdb.app.components.Color
import cz.e_bs.cmi.mdr.pdb.app.components.list.StackedList
import cz.e_bs.cmi.mdr.pdb.app.components.list.ListRow
import cz.e_bs.cmi.mdr.pdb.app.components.list.RowTag
import cz.e_bs.cmi.mdr.pdb.app.components.list.RowNext

object SeznamKriterii:
  case class Kriterium(
      nazev: String,
      kapitola: String,
      bod: String,
      status: String,
      statusColor: Color,
      splneno: Boolean,
      container: HtmlElement = div()
  ) {
    val id = s"${kapitola}${bod}"
  }
  type ViewModel = List[Kriterium]

  private val kritList = new StackedList[Kriterium]

  def render($m: Signal[ViewModel]): HtmlElement =
    div(
      cls := "bg-white shadow overflow-hidden sm:rounded-md",
      kritList($m, _.id) { $i =>
        $i.map { i =>
          ListRow.ViewModel(
            title = i.nazev,
            topRight = RowTag.render(
              $i.map(x => RowTag.ViewModel(x.status, x.statusColor))
            ),
            bottomLeft =
              p(cls := "text-sm text-gray-500", s"${i.kapitola}${i.bod}"),
            bottomRight = emptyNode,
            farRight = RowNext.render,
            containerElement = i.container
          )
        }
      }
    )
