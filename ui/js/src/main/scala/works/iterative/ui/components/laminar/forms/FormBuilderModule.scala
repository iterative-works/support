package works.iterative.ui.components.laminar.forms

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom.html
import com.raquo.laminar.nodes.ReactiveHtmlElement

trait InputField[A]:
  def render: ReactiveHtmlElement[html.Input]

sealed trait Form[A]

object Form:
  case class Input(name: String) extends Form[String]

trait FormBuilderModule:
  def formMessagesResolver: FormMessagesResolver
  def formUIFactory: FormUIFactory
  def buildForm[A](form: Form[A], submit: Observer[Unit]): HtmlFormBuilder[A] =
    HtmlFormBuilder[A](form, submit)

  case class HtmlFormBuilder[A](form: Form[A], submit: Observer[Unit]):
    def renderForm[A](form: Form[A]): HtmlElement = form match
      case Form.Input(name) =>
        formUIFactory.field(
          formUIFactory.label(formMessagesResolver.label(name))()
        )(
          formUIFactory.input(
            name,
            placeholder = formMessagesResolver.placeholder(name)
          )()
        )

    def build: HtmlElement =
      formUIFactory.form(
        onSubmit.preventDefault.mapTo(()) --> submit
      )(renderForm(form))()
