package works.iterative.ui.components.laminar

import com.raquo.laminar.api.L.{*, given}
import works.iterative.ui.components.tailwind.ComponentContext

trait PageComponentsModule:

  val page: PageComponents

  trait PageComponents:
    def container(
        children: Modifier[HtmlElement]*
    ): HtmlElement

    def singleColumn(
        header: Modifier[HtmlElement]
    )(children: Modifier[HtmlElement]*): HtmlElement

    def pageHeader(
        title: Modifier[HtmlElement],
        right: Modifier[HtmlElement] = emptyMod,
        subtitle: Option[Modifier[HtmlElement]] = None
    ): HtmlElement

    /** Visage for clickable text, like links
      */
    def clickable: Modifier[HtmlElement]

trait DefaultPageComponentsModule(using ComponentContext)
    extends PageComponentsModule:

  override val page: PageComponents = new PageComponents:
    override def container(
        children: Modifier[HtmlElement]*
    ): HtmlElement =
      div(
        cls("max-w-7xl mx-auto h-full px-4 sm:px-6 lg:px-8 overflow-y-auto"),
        children
      )

    override def singleColumn(
        header: Modifier[HtmlElement]
    )(children: Modifier[HtmlElement]*): HtmlElement =
      div(
        cls("p-8 bg-gray-100 h-full"),
        header,
        children
      )

    override def pageHeader(
        title: Modifier[HtmlElement],
        right: Modifier[HtmlElement],
        subtitle: Option[Modifier[HtmlElement]] = None
    ): HtmlElement =
      div(
        cls("pb-5 border-b border-gray-200"),
        div(cls("float-right"), right),
        h1(
          cls("text-2xl leading-6 font-medium text-gray-900"),
          title
        ),
        subtitle.map(
          p(
            cls("text-sm font-medium text-gray-500"),
            _
          )
        )
      )

    override def clickable: Modifier[HtmlElement] =
      cls("text-sm font-medium text-indigo-600 hover:text-indigo-400")
