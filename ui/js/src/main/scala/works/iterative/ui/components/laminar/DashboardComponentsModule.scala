package works.iterative.ui.components.laminar

import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.{*, given}
import works.iterative.ui.components.tailwind.laminar.LaminarExtensions.given
import works.iterative.ui.components.tailwind.experimental.ColorDef
import com.raquo.laminar.builders.HtmlTag
import org.scalajs.dom.html.UList
import com.raquo.laminar.nodes.ReactiveHtmlElement
import works.iterative.ui.components.tailwind.ComponentContext
import works.iterative.ui.components.tailwind.experimental.ColorKind

trait DashboardComponentsModule:
  def dashboard: DashboardComponents

  // Only methods that create HtmlElements
  trait DashboardComponents(using ComponentContext):
    def section(label: String, children: Modifier[HtmlElement]): Div
    def cardList(children: Modifier[HtmlElement]): ReactiveHtmlElement[UList]
    def card(
        id: String,
        label: String,
        initials: Div,
        children: Modifier[HtmlElement]
    ): Li
    def cardInitials(
        initials: String,
        color: Signal[ColorKind],
        children: Modifier[HtmlElement]
    ): Div
    def count(label: String, value: Int, color: ColorDef): Span
    def counts(children: HtmlElement*): Span
    def table(
        headers: List[String],
        sections: Seq[ReactiveHtmlElement[org.scalajs.dom.html.TableRow]]
    ): Div
    def tableSectionHeader(
        label: Modifier[HtmlElement],
        cs: Int,
        mods: Modifier[HtmlElement]
    ): ReactiveHtmlElement[org.scalajs.dom.html.TableRow]
    def tableRow(
        label: String,
        mods: Modifier[HtmlElement],
        children: HtmlElement*
    ): ReactiveHtmlElement[org.scalajs.dom.html.TableRow]

trait DefaultDashboardComponentsModule(using ComponentContext)
    extends DashboardComponentsModule:
  override val dashboard: DashboardComponents = new DashboardComponents:
    def section(label: String, children: Modifier[HtmlElement]): Div =
      div(
        cls("mt-3"),
        h2(
          cls("text-gray-500 text-xs font-medium uppercase tracking-wide"),
          label
        ),
        children
      )

    def cardList(children: Modifier[HtmlElement]): ReactiveHtmlElement[UList] =
      ul(
        cls(
          "mt-3 grid gap-5 sm:gap-6 grid-cols-1 sm:grid-cols-2 lg:grid-cols-4"
        ),
        children
      )

    def card(
        id: String,
        label: String,
        initials: Div,
        children: Modifier[HtmlElement]
    ): Li =
      li(
        div(
          cls("col-span-1 flex shadow-sm rounded-md"),
          initials,
          div(
            cls(
              "flex-1 flex items-center justify-between border-t border-r border-b border-gray-200 bg-white rounded-r-md truncate"
            ),
            div(
              cls("flex-1 px-4 py-2 text-sm truncate"),
              div(
                cls("text-gray-900 font-medium hover:text-gray-600 truncate"),
                label
              ),
              p(cls("text-gray-500"), children)
            )
          )
        )
      )

    def cardInitials(
        id: String,
        color: Signal[ColorKind],
        children: Modifier[HtmlElement]
    ): Div =
      div(
        cls(
          "flex-shrink-0 flex flex-col items-center justify-center w-16 text-white text-sm font-medium rounded-l-md"
        ),
        color.map(_(600).bg),
        div(id),
        children
      )

    def count(label: String, value: Int, color: ColorDef): Span =
      span(color.text, title(label), s"${value}")

    def counts(children: HtmlElement*): Span =
      span(interleave(children, span(" / ")))

    def table(
        headers: List[String],
        sections: Seq[ReactiveHtmlElement[org.scalajs.dom.html.TableRow]]
    ): Div =
      div(
        cls("flex flex-col mt-2"),
        div(
          cls(
            "align-middle min-w-full shadow sm:rounded-lg"
          ),
          L.table(
            cls("min-w-full border-separate border-spacing-0"),
            styleAttr("border-spacing: 0"), // Tailwind somehow doesn't work
            thead(
              tr(
                cls("border-t border-gray-200"),
                headers.map(h =>
                  th(
                    cls(
                      "sticky top-0 z-10 px-6 py-3 bg-gray-50 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                    ),
                    h
                  )
                )
              )
            ),
            tbody(cls("bg-white divide-y divide-gray-100"), sections)
          )
        )
      )

    def tableSectionHeader(
        label: Modifier[HtmlElement],
        cs: Int,
        mods: Modifier[HtmlElement]
    ): ReactiveHtmlElement[org.scalajs.dom.html.TableRow] =
      tr(
        td(
          cls("bg-gray-50 border-gray-200 border-t border-b"),
          cls(
            "sticky top-14 z-10 bg-gray-50 px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider"
          ),
          colSpan(cs),
          label
        ),
        mods
      )

    def tableRow(
        label: String,
        mods: Modifier[HtmlElement],
        children: HtmlElement*
    ): ReactiveHtmlElement[org.scalajs.dom.html.TableRow] =
      tr(
        mods,
        td(
          cls(
            "px-6 py-3 max-w-0 w-full whitespace-nowrap text-sm font-medium text-gray-900 truncate"
          ),
          label
        ),
        children.map(e =>
          td(
            cls(
              "whitespace-nowrap px-2 py-2 text-sm text-gray-500"
            ),
            e
          )
        )
      )

    private def interleave(
        a: Seq[HtmlElement],
        separator: => HtmlElement
    ): Seq[HtmlElement] =
      if a.size < 2 then a
      else a.init.flatMap(n => Seq(n, separator)) :+ a.last
