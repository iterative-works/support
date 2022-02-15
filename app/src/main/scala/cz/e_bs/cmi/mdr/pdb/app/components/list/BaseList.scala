package cz.e_bs.cmi.mdr.pdb.app.components.list

import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.Router
import cz.e_bs.cmi.mdr.pdb.app.components.Icons
import cz.e_bs.cmi.mdr.pdb.app.Routes
import cz.e_bs.cmi.mdr.pdb.waypoint.components.Navigator
import com.raquo.laminar.builders.HtmlTag
import org.scalajs.dom

trait BaseList[RowData]:

  type RenderRow = Signal[RowData] => Modifier[HtmlElement]

  inline protected def containerElement: HtmlTag[dom.html.Element] = a

  protected val containerMods: RenderRow
  protected val title: RenderRow
  protected val topRight: RenderRow
  protected val bottomLeft: RenderRow
  protected val bottomRight: RenderRow

  def row($data: Signal[RowData]) =
    li(
      containerElement(
        containerMods($data),
        cls := "block hover:bg-gray-50",
        div(
          cls := "px-4 py-4 sm:px-6 items-center flex",
          div(
            cls := "min-w-0 flex-1 pr-4",
            div(
              cls := "flex items-center justify-between",
              p(
                cls := "text-sm font-medium text-indigo-600 truncate",
                title($data)
              ),
              div(
                cls := "ml-2 flex-shrink-0 flex",
                topRight($data)
              )
            ),
            div(
              cls := "mt-2 sm:flex sm:justify-between",
              bottomLeft($data),
              bottomRight($data)
            )
          ),
          div(
            cls := "flex-shrink-0",
            Icons.solid.`chevron-right`
          )
        )
      )
    )
