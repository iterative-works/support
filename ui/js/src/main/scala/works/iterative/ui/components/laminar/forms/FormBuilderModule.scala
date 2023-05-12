package works.iterative.ui.components.laminar.forms

import zio.prelude.*
import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom.html
import com.raquo.laminar.nodes.ReactiveHtmlElement
import works.iterative.core.UserMessage

trait InputField[A]:
  def render: ReactiveHtmlElement[html.Input]

case class InvalidValue(name: String, message: String => UserMessage)

type Validated[A] = Validation[InvalidValue, A]

sealed trait Form[A]:
  def value: Validated[A]

object Form:
  case class Input(name: String) extends Form[String]:
    override def value: Validated[String] =
      Validation.fail(
        InvalidValue(name, UserMessage("error.value.required", _))
      )

trait FormBuilderModule:
  def formMessagesResolver: FormMessagesResolver
  def formUIFactory: FormUIFactory
  def buildForm[A](form: Form[A], submit: Observer[A]): HtmlFormBuilder[A] =
    HtmlFormBuilder[A](form, submit)

  case class HtmlFormBuilder[A](form: Form[A], submit: Observer[A]):
    def renderForm[A](form: Form[A]): HtmlElement = form match
      case i @ Form.Input(name) =>
        val userLabel = formMessagesResolver.label(name)
        formUIFactory.field(
          formUIFactory.label(userLabel)()
        )(
          formUIFactory.input(
            name,
            placeholder = formMessagesResolver.placeholder(name)
          )(),
          i.value.fold(
            msgs =>
              msgs
                .map(msg =>
                  formUIFactory.validationError(
                    formMessagesResolver.message(msg.message(userLabel))
                  )
                )
                .toList,
            _ => List.empty[HtmlMod]
          )
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
