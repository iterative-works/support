package works.iterative.ui.components.tailwind.form

import com.raquo.laminar.api.L.{*, given}

import zio.prelude.Validation
import works.iterative.ui.components.tailwind.ComponentContext

class Switch[V](using codec: FormCodec[V, Boolean], ctx: ComponentContext[_])
    extends FormInput[V]:
  def render(
      property: Property[V],
      updates: Observer[Validated[V]]
  ): HtmlElement =
    val initialValue = property.value.map(codec.toForm).getOrElse(false)
    val currentValue = Var(initialValue)
    div(
      currentValue.signal.map(codec.toValue) --> updates,
      cls := "flex items-center",
      button(
        tpe := "button",
        cls := "relative inline-flex flex-shrink-0 h-6 w-11 border-2 border-transparent rounded-full cursor-pointer transition-colors ease-in-out duration-200 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500",
        cls <-- currentValue.signal.map(v =>
          if v then "bg-indigo-600" else "bg-gray-200"
        ),
        role := "switch",
        dataAttr("aria-checked") := "false",
        dataAttr("aria-labelledby") := "active-only-label",
        span(
          dataAttr("aria-hidden") := "true",
          cls := "pointer-events-none inline-block h-5 w-5 rounded-full bg-white shadow transform ring-0 transition ease-in-out duration-200",
          cls <-- currentValue.signal.map(v =>
            if v then "translate-x-5" else "translate-x-0"
          )
        ),
        onClick.compose(
          _.sample(currentValue.signal).map(v => !v)
        ) --> currentValue
      ),
      ctx.messages
        .get(property.name)
        .map(name =>
          span(
            cls := "ml-3",
            idAttr := "active-only-label",
            span(
              cls := "text-sm font-medium text-gray-900",
              name
            )
          )
        )
    )
