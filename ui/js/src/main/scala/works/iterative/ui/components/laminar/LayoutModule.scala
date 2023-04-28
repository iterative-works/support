package works.iterative.ui.components.laminar

import com.raquo.laminar.api.L.{*, given}
import works.iterative.ui.components.tailwind.ComponentContext

trait LayoutModule:
  def layout: LayoutComponents

  trait LayoutComponents:
    def card(content: Modifier[HtmlElement]*): HtmlElement

trait DefaultLayoutModule(using ctx: ComponentContext) extends LayoutModule:
  override val layout: LayoutComponents = new LayoutComponents:
    override def card(content: Modifier[HtmlElement]*): HtmlElement =
      div(cls("bg-white shadow px-4 py-5 sm:rounded-lg sm:p-6"), content)
