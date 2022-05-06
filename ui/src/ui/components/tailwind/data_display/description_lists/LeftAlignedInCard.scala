package works.iterative.ui.components.tailwind.data_display.description_lists

import com.raquo.laminar.api.L.{*, given}
import works.iterative.ui.UIString
import works.iterative.ui.components.tailwind.TimeUtils
import java.time.LocalDate
import works.iterative.ui.components.tailwind.BaseHtmlComponent

case class OptionalLabeledValue(
    label: UIString,
    v: Option[Modifier[HtmlElement]]
)

object OptionalLabeledValue:
  def makeValue[V](label: UIString, v: V)(using
      r: OptionalValueRender[V]
  ): OptionalLabeledValue = OptionalLabeledValue(label, r.render(v))

trait OptionalValueRender[V]:
  def render(v: V): Option[Modifier[HtmlElement]]

object OptionalValueRender:
  given stringValue: OptionalValueRender[String] with
    def render(v: String): Option[Modifier[HtmlElement]] = Some(
      v: Modifier[HtmlElement]
    )
  given dateValue: OptionalValueRender[LocalDate] with
    def render(v: LocalDate): Option[Modifier[HtmlElement]] = Some(
      TimeUtils.formatDate(v)
    )
  given optionValue[T](using
      r: OptionalValueRender[T]
  ): OptionalValueRender[Option[T]] with
    def render(v: Option[T]): Option[Modifier[HtmlElement]] =
      v.flatMap(r.render)

case class LeftAlignedInCard(
    title: String,
    subtitle: String,
    data: List[OptionalLabeledValue]
)

object LeftAlignedInCard:
  given leftAlignedInCardComponent: BaseHtmlComponent[LeftAlignedInCard] with
    extension (d: LeftAlignedInCard)
      def element: HtmlElement =
        div(
          cls := "bg-white shadow overflow-hidden sm:rounded-lg",
          div(
            cls := "px-4 py-5 sm:px-6",
            h3(cls := "text-lg leading-6 font-medium text-gray-900", d.title),
            p(cls := "mt-1 max-w-2xl text-sm text-gray-500", d.subtitle)
          ),
          div(
            cls := "border-t border-gray-200 px-4 py-5 sm:p-0",
            dl(
              cls := "sm:divide-y sm:divide-gray-200",
              d.data.collect { case OptionalLabeledValue(label, Some(body)) =>
                div(
                  cls := "py-4 sm:py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6",
                  dt(cls := "text-sm font-medium text-gray-500", label),
                  dd(
                    cls := "mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2",
                    body
                  )
                )
              }
            )
          )
        )
