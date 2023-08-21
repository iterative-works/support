package works.iterative.ui.components.laminar.forms

import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
import zio.prelude.Validation

trait FormBuilderModule:
  def buildForm[A](form: Form[A], submit: Observer[A]): HtmlFormBuilder[A] =
    HtmlFormBuilder[A](form, submit)

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
