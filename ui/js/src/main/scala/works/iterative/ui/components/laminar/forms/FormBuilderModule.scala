package works.iterative.ui.components.laminar.forms

import zio.prelude.Validation
import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom.html
import com.raquo.laminar.nodes.ReactiveHtmlElement
import works.iterative.core.UserMessage
import works.iterative.core.PlainMultiLine
import com.raquo.airstream.core.Signal
import works.iterative.ui.components.tailwind.HtmlRenderable.given
import works.iterative.ui.components.tailwind.ComponentContext
import works.iterative.core.MessageCatalogue

trait FormBuilderModule(using fctx: FormBuilderContext):
  def buildForm[A](form: Form[A], submit: Observer[A]): HtmlFormBuilder[A] =
    HtmlFormBuilder[A](form, submit)

  case class HtmlFormBuilder[A](form: Form[A], submit: Observer[A]):
    def build(initialValue: Option[A]): FormComponent[A] =
      val f = form.build(initialValue)
      new FormComponent[A]:
        override val validated: Signal[Validated[A]] = f.validated
        override val element: HtmlElement =
          fctx.formUIFactory.form(
            onSubmit.preventDefault.compose(_.sample(f.validated).collect {
              case Validation.Success(_, value) => value
            }) --> submit
          )(f.element)(
            fctx.formUIFactory.submit(
              fctx.formMessagesResolver.label("submit")
            )(disabled <-- f.validated.map(_.fold(_ => true, _ => false)))
          )
