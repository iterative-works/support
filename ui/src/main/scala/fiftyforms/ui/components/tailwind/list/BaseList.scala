package fiftyforms.ui.components.tailwind
package list

import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.Router
import com.raquo.laminar.builders.HtmlTag
import org.scalajs.dom

trait ListItem:
  def key: String
  def title: Modifier[HtmlElement]
  def topRight: Modifier[HtmlElement]
  def bottomLeft: Modifier[HtmlElement]
  def bottomRight: Modifier[HtmlElement]

trait ListRenderable[Item]:
  extension (x: Item) def asItem: ListItem

trait Navigable[Item]:
  extension (x: Item) def navigate: Modifier[HtmlElement]

object BaseList:
  enum Color:
    case Green, Yellow, Red

  case class IconText(text: HtmlElement, icon: SvgElement)
  case class Tag(text: String, color: Color)
  case class Row(
      id: String,
      title: String,
      tag: Tag,
      leftProps: List[IconText],
      rightProp: IconText
  )

  trait AsRow[Data]:
    extension (d: Data) def asRow: Row

  class RowListItem(d: Row) extends ListItem:

    def key: String = d.id

    def title: Modifier[HtmlElement] = d.title

    def topRight: Modifier[HtmlElement] =
      inline def colorClass(color: Color): (String, Boolean) =
        val c = color.toString.toLowerCase
        s"bg-$c-100 text-$c-800" -> (d.tag.color == color)

      inline def colors = Map(Color.values.map(colorClass(_)): _*)

      p(
        cls := "px-2 inline-flex text-xs leading-5 font-semibold rounded-full",
        cls := colors,
        d.tag.text
      )

    def bottomLeft: Modifier[HtmlElement] =
      div(
        cls := "sm:flex",
        d.leftProps.zipWithIndex.map { case (i, idx) =>
          p(
            cls := Map("mt-2 sm:mt-0 sm:ml-6" -> (idx == 0)),
            cls := "flex items-center text-sm text-gray-500",
            i.icon,
            i.text
          )
        }
      )

    def bottomRight: Modifier[HtmlElement] =
      div(
        cls := "mt-2 flex items-center text-sm text-gray-500 sm:mt-0",
        d.rightProp.icon,
        d.rightProp.text
      )

  object Row:
    given asRowRenderable[T: AsRow]: ListRenderable[T] with
      extension (d: T) def asItem = new RowListItem(d.asRow)

  end Row

class BaseList[RowData: ListRenderable]:

  protected def containerElement: HtmlTag[dom.html.Element] = div
  protected def containerMods(rowData: RowData): Modifier[HtmlElement] =
    emptyMod
  protected def farRight: Modifier[HtmlElement] = emptyMod

  def render($data: Signal[List[RowData]]): HtmlElement =
    ul(
      role := "list",
      cls := "divide-y divide-gray-200",
      children <-- $data.split(_.asItem.key)((_, d, _) => row(d))
    )

  private def row(d: RowData): HtmlElement =
    val data = d.asItem
    li(
      containerElement(
        containerMods(d),
        cls := "block hover:bg-gray-50",
        div(
          cls := "px-4 py-4 sm:px-6 items-center flex",
          div(
            cls := "min-w-0 flex-1 pr-4",
            div(
              cls := "flex items-center justify-between",
              p(
                cls := "text-sm font-medium text-indigo-600 truncate",
                data.title
              ),
              div(
                cls := "ml-2 flex-shrink-0 flex",
                data.topRight
              )
            ),
            div(
              cls := "mt-2 sm:flex sm:justify-between",
              data.bottomLeft,
              data.bottomRight
            )
          ),
          farRight
        )
      )
    )

trait NavigableList[RowData: Navigable, Page](using router: Router[Page])
    extends BaseList[RowData]:

  override protected def containerElement: HtmlTag[dom.html.Element] = a
  override protected def containerMods(
      rowData: RowData
  ): Modifier[HtmlElement] =
    rowData.navigate
  override protected def farRight: Modifier[HtmlElement] =
    div(
      cls := "flex-shrink-0",
      Icons.solid.`chevron-right`().amend(svg.cls := "text-gray-400")
    )
