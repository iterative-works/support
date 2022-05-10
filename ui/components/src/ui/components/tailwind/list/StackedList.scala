package works.iterative.ui.components.tailwind
package list

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import com.raquo.laminar.nodes.ReactiveHtmlElement
import works.iterative.ui.components.headless.Items
import works.iterative.ui.components.headless.GroupedItems
import works.iterative.ui.components.headless.Toggle

class StackedList[Item: AsListRow]:
  import StackedList.*
  def apply(items: List[Item]): ReactiveHtmlElement[dom.html.UList] =
    ul(
      role := "list",
      cls := "divide-y divide-gray-200",
      items.map(d => d.asListRow.element)
    )

  def withMod(
      items: List[Item]
  ): Modifier[HtmlElement] => ReactiveHtmlElement[dom.html.UList] = mods =>
    ul(
      role := "list",
      cls := "divide-y divide-gray-200",
      mods,
      items.map(d => d.asListRow.element)
    )

  def grouped(items: List[Item], groupBy: Item => String): List[HtmlElement] =
    items.groupBy(groupBy).to(List).sortBy(_._1).map { case (c, i) =>
      withHeader(c)(withMod(i))
    }

object StackedList:
  def withHeader(header: String)(
      content: Modifier[HtmlElement] => ReactiveHtmlElement[dom.html.UList]
  ): HtmlElement =
    div(
      cls("relative"),
      h3(
        cls(
          "z-10 sticky top-0 border-t border-b border-gray-200 bg-gray-50 px-6 py-1 text-sm font-medium text-gray-500"
        ),
        header
      ),
      content(cls("relative"))
    )

object StackedListWithRightJustifiedSecondColumn:
  opaque type Title = ReactiveHtmlElement[dom.html.Paragraph]
  opaque type Tag = ReactiveHtmlElement[dom.html.Paragraph]
  opaque type LeftProp = ReactiveHtmlElement[dom.html.Paragraph]
  opaque type RightProp = Div

  case class Item(
      title: Title,
      tag: Tag,
      leftProps: Seq[LeftProp] = Nil,
      rightProp: Option[RightProp] = None
  )

  def title(text: String): Title =
    p(
      cls := "text-sm font-medium text-indigo-600 truncate",
      text
    )

  def tag(text: String, color: Color): Tag =
    p(
      cls("px-2 inline-flex text-xs leading-5 font-semibold rounded-full"),
      text
    )

  def leftProp(text: String, icon: Option[SvgElement] = None): LeftProp =
    p(
      cls(
        "mt-2 flex items-center text-sm text-gray-500 sm:mt-0 sm:ml-6"
      ),
      icon,
      text
    )

  def rightProp(text: String, icon: Option[SvgElement] = None): RightProp =
    div(
      cls("mt-2 flex items-center text-sm text-gray-500 sm:mt-0"),
      icon,
      text
    )

  private def item(i: Item): Div = item(i, None)

  private def item(i: Item, extraClasses: Option[String]): Div =
    div(
      cls := "px-4 py-4 sm:px-6 items-center flex",
      extraClasses.map(cls(_)),
      div(
        cls := "min-w-0 flex-1 pr-4",
        div(
          cls := "flex items-center justify-between",
          i.title,
          div(cls := "ml-2 flex-shrink-0 flex", i.tag)
        ),
        div(
          cls := "mt-2 sm:flex sm:justify-between",
          div(
            cls("sm:flex"),
            i.leftProps
          ),
          i.rightProp
        )
      )
    )

  private def headerFrame(text: String): Seq[HtmlElement] => Div =
    content =>
      Toggle(ctx =>
        div(
          cls("relative"),
          h3(
            cls(
              "z-10 sticky top-0 border-t border-b border-gray-200 bg-gray-50 px-6 py-1 text-sm font-medium text-gray-500"
            ),
            text,
            ctx.trigger
          ),
          children <-- ctx.toggle(content)
        )
      )

  private def frame: Seq[HtmlElement] => Div =
    el =>
      div(
        cls("bg-white shadow overflow-hidden sm:rounded-md"),
        ul(role("list"), cls("divide-y divide-gray-200"), el)
      )

  def apply[A](f: this.type => A => Item): Items[A] =
    Items(frame, item).contramap(f(this))

  def grouped[A](f: this.type => A => Item): GroupedItems[String, A] =
    GroupedItems(
      frame,
      k => Items(headerFrame(k), item(_, Some("relative"))).contramap(f(this))
    )