package works.iterative.ui.components.laminar

import com.raquo.laminar.api.L.{*, given}
import works.iterative.ui.model.color.ColorKind
import works.iterative.ui.components.tailwind.ComponentContext
import works.iterative.ui.components.tailwind.laminar.LaminarExtensions.given
import works.iterative.ui.model.Tag

trait GenericComponentsModule:

  def generic: GenericComponents

  trait GenericComponents:
    def tag(name: String, color: ColorKind): HtmlElement
    def tag(t: Tag): HtmlElement = tag(t.value, t.color)

trait DefaultGenericComponentsModule(using ComponentContext)
    extends GenericComponentsModule:
  override val generic: GenericComponents = new GenericComponents:
    override def tag(name: String, color: ColorKind): HtmlElement =
      p(
        cls(
          "px-2 inline-flex text-xs leading-5 font-semibold rounded-full"
        ),
        color(800).text,
        color(100).bg,
        name
      )
