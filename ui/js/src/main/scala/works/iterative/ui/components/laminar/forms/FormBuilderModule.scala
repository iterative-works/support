package works.iterative.ui.components.laminar.forms

import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
import zio.prelude.Validation
import works.iterative.ui.components.ComponentContext

trait FormBuilderModule:
  def buildForm[A](
      form: Form[A],
      events: Observer[Form.Event[A]],
      control: EventStream[Form.Control] = EventStream.empty
  ): HtmlFormBuilder[A] =
    HtmlFormBuilder[A](form, events, control)

  def buildForm[A](
      schema: FormSchema[A],
      submit: Observer[A]
  ): HtmlFormSchemaBuilder[A] =
    HtmlFormSchemaBuilder[A](schema, submit)

  case class HtmlFormBuilder[A](
      form: Form[A],
      events: Observer[Form.Event[A]],
      control: EventStream[Form.Control] = EventStream.empty
  ):
    def build(initialValue: Option[A])(using
        fctx: FormBuilderContext
    ): FormComponent[A] =
      val buttonsDisabled = Var(false)
      val f = form.build(initialValue)
      f.wrap(
        fctx.formUIFactory.form(
          onSubmit.preventDefault.compose(_.sample(f.validated).collect {
            case Validation.Success(_, value) => Form.Event.Submitted(value)
          }) --> events,
          control --> {
            case Form.Control.DisableButtons => buttonsDisabled.set(true)
            case Form.Control.EnableButtons  => buttonsDisabled.set(false)
          }
        )(_)(
          fctx.formUIFactory.cancel(fctx.formMessagesResolver.label("cancel"))(
            disabled <-- buttonsDisabled.signal,
            onClick.preventDefault.mapTo(Form.Event.Cancelled) --> events
          ),
          fctx.formUIFactory.submit(
            fctx.formMessagesResolver.label("submit")
          )(
            disabled <-- f.validated.combineWithFn(buttonsDisabled.signal)(
              (v, d) => v.fold(_ => true, _ => d)
            )
          )
        )
      )

  // TODO: update according to Form builder above, replace Form builder
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
      import works.iterative.ui.components.laminar.HtmlRenderable.given
      schema match
        case FormSchema.Unit => FormComponent.empty
        case Section(name, inner) =>
          val desc = SectionDescriptor(name)
          buildForm(inner)(initialValue).wrap(
            fctx.formUIFactory
              .section(desc.title, desc.subtitle.map(textToTextNode))(_*)
          )
        case Control(name, required, decode, validation, inputType) =>
          val desc = FieldDescriptor(name)
          FieldBuilder
            .Input(
              desc,
              initialValue.map(decode(_)),
              validation,
              inputType
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
