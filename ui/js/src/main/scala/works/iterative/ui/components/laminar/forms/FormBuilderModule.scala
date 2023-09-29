package works.iterative.ui.components.laminar.forms

import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
import zio.prelude.Validation
import works.iterative.ui.components.ComponentContext

trait FormBuilderModule:
  def buildForm[A](form: Form[A], submit: Observer[A]): HtmlFormBuilder[A] =
    HtmlFormBuilder[A](form, submit)

  def buildForm[A](
      schema: FormSchema[A],
      submit: Observer[A]
  ): HtmlFormSchemaBuilder[A] =
    HtmlFormSchemaBuilder[A](schema, submit)

  case class HtmlFormBuilder[A](form: Form[A], submit: Observer[A]):
    def build(initialValue: Option[A])(using
        fctx: FormBuilderContext
    ): FormComponent[A] =
      val f = form.build(initialValue)
      f.wrap(
        fctx.formUIFactory.form(
          onSubmit.preventDefault.compose(_.sample(f.validated).collect {
            case Validation.Success(_, value) => value
          }) --> submit
        )(_)(
          fctx.formUIFactory.submit(
            fctx.formMessagesResolver.label("submit")
          )(
            disabled <-- f.validated.map(_.fold(_ => true, _ => false))
          )
        )
      )

  case class HtmlFormSchemaBuilder[A](
      schema: FormSchema[A],
      submit: Observer[A]
  ):
    def build(initialValue: Option[A])(using
        fctx: FormBuilderContext,
        cctx: ComponentContext[?]
    ): FormComponent[A] =
      val f = buildForm(schema)(initialValue)
      f.wrap(
        fctx.formUIFactory.form(
          onSubmit.preventDefault.compose(_.sample(f.validated).collect {
            case Validation.Success(_, value) => value
          }) --> submit
        )(_)(
          fctx.formUIFactory.submit(
            fctx.formMessagesResolver.label("submit")
          )(
            disabled <-- f.validated.map(_.fold(_ => true, _ => false))
          )
        )
      )

    def buildForm[A](schema: FormSchema[A])(initialValue: Option[A])(using
        fctx: FormBuilderContext,
        cctx: ComponentContext[?]
    ): FormComponent[A] =
      import FormSchema.*
      import works.iterative.ui.components.laminar.LaminarExtensions.*
      import works.iterative.ui.components.laminar.HtmlRenderable.given
      schema match
        case FormSchema.Unit => FormComponent.empty
        case Section(name, inner) =>
          val desc = SectionDescriptor(name)
          buildForm(inner)(initialValue).wrap(
            fctx.formUIFactory
              .section(desc.title, desc.subtitle.map(textToTextNode))(_*)
          )
        case Control(name, required, decode, validation) =>
          val desc = FieldDescriptor(name)
          FieldBuilder
            .Input(
              desc,
              initialValue.map(decode(_)),
              validation
            )
            .wrap(
              fctx.formUIFactory.field(
                fctx.formUIFactory
                  .label(desc.label, required = required)()
              )(
                _,
                desc.help.map(t => fctx.formUIFactory.fieldHelp(t.render))
              )
            )
        case z @ Zip(left, right) =>
          val leftComponent = buildForm(left)(initialValue.map(z.toLeft))
          val rightComponent = buildForm(right)(initialValue.map(z.toRight))
          leftComponent.zip(rightComponent)
        case BiMap(inner, to, from) =>
          buildForm(inner)(initialValue.map(from)).map(to)
