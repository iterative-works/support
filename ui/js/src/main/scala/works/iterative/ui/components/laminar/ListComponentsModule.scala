package works.iterative.ui.components.laminar

import com.raquo.laminar.api.L.{*, given}
import works.iterative.ui.components.tailwind.ComponentContext
import works.iterative.ui.model.color.ColorKind
import works.iterative.ui.components.tailwind.laminar.LaminarExtensions.given
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html.Paragraph
import com.raquo.domtypes.generic.codecs.BooleanAsTrueFalseStringCodec
import com.raquo.laminar.nodes.TextNode

trait ListComponentsModule:

  def list: ListComponents

  trait ListComponents(using ComponentContext):
    def label(text: String, color: ColorKind): HtmlElement
    def item(
        title: String,
        subtitle: Option[String],
        right: Modifier[HtmlElement] = emptyMod,
        avatar: Option[Modifier[HtmlElement]] = None
    ): Li
    def unordered(
        children: Modifier[HtmlElement]
    ): ReactiveHtmlElement[org.scalajs.dom.html.UList]
    def listSection(header: String, list: HtmlElement): Div
    def navigation(sections: Modifier[HtmlElement]): HtmlElement

trait DefaultListComponentsModule(using ComponentContext)
    extends ListComponentsModule:
  self: GenericComponentsModule =>

  override val list: ListComponents = new ListComponents:
    override def label(
        text: String,
        color: ColorKind
    ): HtmlElement = generic.tag(text, color)

    override def item(
        title: String,
        subtitle: Option[String],
        right: Modifier[HtmlElement] = emptyMod,
        avatar: Option[Modifier[HtmlElement]] = None
    ): Li =
      li(
        cls("group"),
        div(
          cls(
            "bg-white relative px-6 py-5 flex items-center space-x-3 hover:bg-gray-50 focus-within:ring-2 focus-within:ring-inset focus-within:ring-pink-500"
          ),
          avatar.map(a =>
            div(
              cls("flex-shrink-0"),
              div(
                cls(
                  "rounded-full text-indigo-200 bg-indigo-600 flex items-center justify-center w-10 h-10"
                ),
                a
              )
            )
          ),
          div(
            cls("flex-1 min-w-0"),
            p(
              cls("text-sm font-medium text-gray-900"),
              title,
              span(cls("float-right"), right)
            ),
            subtitle.map(st =>
              p(
                cls("text-sm text-gray-500 truncate"),
                st
              )
            )
          )
        )
      )

    override def unordered(
        children: Modifier[HtmlElement]
    ): ReactiveHtmlElement[org.scalajs.dom.html.UList] =
      ul(
        cls("relative z-0 divide-y divide-gray-200"),
        role("list"),
        children
      )

    override def listSection(
        header: String,
        list: HtmlElement
    ): Div =
      div(
        cls("relative"),
        div(
          cls(
            "z-10 sticky top-0 border-t border-b border-gray-200 bg-gray-50 px-6 py-1 text-sm font-medium text-gray-500"
          ),
          header
        ),
        list
      )

    override def navigation(sections: Modifier[HtmlElement]): HtmlElement =
      nav(
        cls("flex-1 min-h-0 overflow-y-auto"),
        sections
      )
