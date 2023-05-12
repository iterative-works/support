package works.iterative.ui.components.laminar.forms

import com.raquo.laminar.api.L.{*, given}
import works.iterative.ui.components.tailwind.HtmlRenderable.given

sealed trait Form[A] extends FormBuilder[A]

object Form:
  case class Input[A: FieldBuilder](desc: FieldDescriptor)(using
      fctx: FormBuilderContext
  ) extends Form[A]:
    override def build(initialValue: Option[A]): FormComponent[A] =
      val field = summon[FieldBuilder[A]]
      val inputComponent = field.build(desc, initialValue)
      new FormComponent[A]:
        override val validated: Signal[Validated[A]] = inputComponent.validated
        override val element: HtmlElement =
          fctx.formUIFactory.field(
            fctx.formUIFactory.label(desc.label, required = field.required)()
          )(
            inputComponent.element,
            desc.help.map(t => fctx.formUIFactory.fieldHelp(t.render))
          )
