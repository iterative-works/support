package works.iterative.ui.components.tailwind.data_display.description_lists

import com.raquo.laminar.api.L.{*, given}
import works.iterative.ui.UIString
import works.iterative.ui.components.tailwind.TimeUtils
import java.time.LocalDate
import works.iterative.ui.components.tailwind.BaseHtmlComponent
import works.iterative.ui.components.tailwind.HtmlRenderable
import works.iterative.ui.components.tailwind.form.ActionButtons
import works.iterative.ui.components.tailwind.HtmlComponent
import works.iterative.ui.components.tailwind.form.ActionButton

case class LeftAlignedInCard[A](
    title: String,
    subtitle: String,
    data: List[LeftAlignedInCard.OptionalLabeledValue],
    // TODO: a version without actions
    actions: List[ActionButton[A]]
)

object LeftAlignedInCard:
  case class OptionalLabeledValue(
      label: UIString,
      v: Option[Modifier[HtmlElement]]
  )

  trait AsValue[V]:
    def toLabeled(n: UIString, v: V): OptionalLabeledValue
    extension (v: V)
      def labeled(n: UIString): OptionalLabeledValue = toLabeled(n, v)

  given optionValue[V: HtmlRenderable]: AsValue[Option[V]] with
    def toLabeled(n: UIString, v: Option[V]): OptionalLabeledValue =
      OptionalLabeledValue(n, v.map(_.render))

  given [V: HtmlRenderable]: AsValue[V] with
    def toLabeled(n: UIString, v: V): OptionalLabeledValue =
      OptionalLabeledValue(n, Some(v.render))

  given leftAlignedInCardComponent[A](using
      HtmlComponent[_, ActionButtons[A]]
  ): BaseHtmlComponent[LeftAlignedInCard[A]] =
    (d: LeftAlignedInCard[A]) =>
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
        ),
        if d.actions.nonEmpty then
          Some(
            div(
              cls := "border-t border-gray-200 px-4 py-5 sm:p-0",
              div(
                cls := "px-4 py-5 sm:px-6",
                ActionButtons(d.actions).element
              )
            )
          )
        else None
      )
