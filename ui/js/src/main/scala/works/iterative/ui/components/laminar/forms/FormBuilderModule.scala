package works.iterative.ui.components.laminar.forms

import zio.prelude.*
import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom.html
import com.raquo.laminar.nodes.ReactiveHtmlElement
import works.iterative.core.UserMessage

trait InputField[A]:
  def render: ReactiveHtmlElement[html.Input]

type Validated[A] = Validation[UserMessage, A]

sealed trait Form[A]:
  def value: Validated[A]

object Form:
  case class Input(name: String) extends Form[String]:
    override def value: Validated[String] =
      Validation.fail(UserMessage("error.invalid.value"))

trait FormBuilderModule:
  def formMessagesResolver: FormMessagesResolver
  def formUIFactory: FormUIFactory
  def buildForm[A](form: Form[A], submit: Observer[A]): HtmlFormBuilder[A] =
    HtmlFormBuilder[A](form, submit)

  case class HtmlFormBuilder[A](form: Form[A], submit: Observer[A]):
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
        onSubmit.preventDefault.map(_ => form.value).collect {
          case Validation.Success(_, value) => value
        } --> submit
      )(renderForm(form))(
        formUIFactory.submit(
          formMessagesResolver.label("submit")
        )
      )
