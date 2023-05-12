package works.iterative.ui.components.laminar.forms

import zio.prelude.*
import com.raquo.laminar.api.L.{*, given}
import works.iterative.ui.components.tailwind.HtmlRenderable.given

sealed trait Form[A] extends FormBuilder[A]

object Form:
  case class Section[A](desc: SectionDescriptor)(content: Form[A])(using
      fctx: FormBuilderContext
  ) extends Form[A]:
    override def build(initialValue: Option[A]): FormComponent[A] =
      content
        .build(initialValue)
        .wrap(
          fctx.formUIFactory.section(
            desc.title,
            desc.subtitle.map(textToTextNode(_))
          )(_*)
        )

  case class Zip[A, B](left: Form[A], right: Form[B])(using
      fctx: FormBuilderContext
  ) extends Form[(A, B)]:
    override def build(initialValue: Option[(A, B)]): FormComponent[(A, B)] =
      val leftComponent = left.build(initialValue.map(_._1))
      val rightComponent = right.build(initialValue.map(_._2))
      leftComponent <*> rightComponent

  case class BiMap[A, B](form: Form[A], f: A => B, g: B => A)(using
      fctx: FormBuilderContext
  ) extends Form[B]:
    override def build(initialValue: Option[B]): FormComponent[B] =
      form.build(initialValue.map(g)).map(f)

  case class Input[A: FieldBuilder](desc: FieldDescriptor)(using
      fctx: FormBuilderContext
  ) extends Form[A]:
    override def build(initialValue: Option[A]): FormComponent[A] =
      val field = summon[FieldBuilder[A]]
      field
        .build(desc, initialValue)
        .wrap(
          fctx.formUIFactory.field(
            fctx.formUIFactory.label(desc.label, required = field.required)()
          )(
            _,
            desc.help.map(t => fctx.formUIFactory.fieldHelp(t.render))
          )
        )

  extension [A](f: Form[A])
    def zip[B](other: Form[B])(using fctx: FormBuilderContext): Form[(A, B)] =
      Zip(f, other)
