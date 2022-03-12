package works.iterative.ui.components.tailwind.form

import com.raquo.laminar.api.L.{*, given}

object FormHeader:
  case class ViewModel(header: String, description: String)
  def apply(m: ViewModel): HtmlElement =
    div(
      h3(cls := "text-lg leading-6 font-medium text-gray-900", m.header),
      p(cls := "mt-1 max-w-2xl text-sm text-gray-500", m.description)
    )
