package works.iterative.ui.components.tailwind.data_display.description_lists

import com.raquo.laminar.api.L.*
import works.iterative.ui.components.ComponentContext
import works.iterative.ui.components.laminar.HtmlRenderable
import works.iterative.ui.components.tailwind.Icons

type ValueContent = String | Modifier[HtmlElement]
type OptionalValueContent = ValueContent | Option[ValueContent]

case class LabeledValue(label: String, body: OptionalValueContent):
    def content: Option[Modifier[HtmlElement]] = body match
        case Some(s: String)                => Some(s)
        case Some(m: Modifier[HtmlElement]) => Some(m)
        case s: String                      => Some(s)
        case m: Modifier[?]                 => Some(m.asInstanceOf[Modifier[HtmlElement]])
        case _                              => None
end LabeledValue

object LabeledValue:
    given renderableToLabeledValue[V: HtmlRenderable](using
        cctx: ComponentContext[?]
    ): Conversion[(String, V), LabeledValue] with
        def apply(v: (String, V)) =
            LabeledValue(cctx.messages(v._1), Some(v._2.render))
    end renderableToLabeledValue

    given optionalRenderableToLabeledValue[V: HtmlRenderable](using
        cctx: ComponentContext[?]
    ): Conversion[(String, Option[V]), LabeledValue] with
        def apply(v: (String, Option[V])) =
            LabeledValue(cctx.messages(v._1), v._2.map(_.render))
    end optionalRenderableToLabeledValue
end LabeledValue

// TODO: drop UI string, use MessageId, use builder like FormBuilder
case class LeftAlignedInCard[A](
    title: String,
    subtitle: String,
    data: List[LabeledValue],
    actions: Option[Modifier[HtmlElement]] = None,
    close: Option[Modifier[HtmlElement]] = None
):

    private def renderDataRow(value: LabeledValue): Option[HtmlElement] =
        value.content.map(c =>
            div(
                cls := "py-4 sm:py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6",
                dt(cls := "text-sm font-medium text-gray-500", value.label),
                dd(
                    cls := "mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2",
                    c
                )
            )
        )

    def element: HtmlElement =
        div(
            cls := "relative bg-white shadow overflow-hidden sm:rounded-lg",
            close.map(mods =>
                div(
                    cls("absolute top-0 right-0 hidden pt-4 pr-4 sm:block"),
                    button(
                        cls(
                            "rounded-md bg-white text-gray-400 hover:text-gray-500 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
                        ),
                        mods,
                        span(cls("sr-only"), "Close"),
                        Icons.outline.x("h-6 w-6", "1.5")
                    )
                )
            ),
            div(
                cls := "px-4 py-5 sm:px-6",
                h3(cls := "text-lg leading-6 font-medium text-gray-900", title),
                p(cls := "mt-1 max-w-2xl text-sm text-gray-500", subtitle)
            ),
            div(
                cls := "border-t border-gray-200 px-4 py-5 sm:p-0",
                dl(
                    cls := "sm:divide-y sm:divide-gray-200",
                    data.map(renderDataRow).collect { case Some(el) => el }
                )
            ),
            actions.map(acts =>
                div(
                    cls := "border-t border-gray-200 px-4 py-5 sm:p-0",
                    div(cls := "px-4 py-5 sm:px-6", acts)
                )
            )
        )
end LeftAlignedInCard
