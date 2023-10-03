package works.iterative.ui.components.laminar.forms

import zio.prelude.Validation
import com.raquo.laminar.api.L.*
import works.iterative.ui.components.laminar.HtmlRenderable.given

sealed trait Form[A] extends FormBuilder[A]

object Form:
  enum Event[+A]:
    case Submitted(a: A) extends Event[A]
    case Cancelled extends Event[Nothing]

  enum Control:
    case DisableButtons, EnableButtons

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

  case class Zip[A, B <: Tuple](
      left: Form[A],
      right: Form[B]
  ) extends Form[A *: B]:
    override def build(
        initialValue: Option[A *: B]
    ): FormComponent[A *: B] =
      val leftComponent = left.build(initialValue.map(_.head))
      val rightComponent = right.build(initialValue.map(_.tail))
      leftComponent.zip(rightComponent)

  case class BiMap[A, B](form: Form[A], f: A => B, g: B => A) extends Form[B]:
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

  case object Empty extends Form[EmptyTuple]:
    override def build(
        initialValue: Option[EmptyTuple]
    ): FormComponent[EmptyTuple] =
      FormComponent(Val(Validation.succeed(EmptyTuple)), Nil)

  extension [A <: Tuple](tail: Form[A])
    def prepend[B](head: Form[B])(using
        fctx: FormBuilderContext
    ): Form[B *: A] =
      Zip[B, A](head, tail)

  extension [A](f: Form[A])
    def +:[B <: Tuple](other: Form[B])(using
        fctx: FormBuilderContext
    ): Form[A *: B] =
      Zip(f, other)

    def zip[B <: Tuple](other: Form[B])(using
        FormBuilderContext
    ): Form[A *: B] =
      Zip(f, other)
