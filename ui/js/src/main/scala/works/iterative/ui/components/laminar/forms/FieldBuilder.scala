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

trait FieldBuilder[A]:
  def required: Boolean
  def build(
      fieldDescriptor: FieldDescriptor,
      initialValue: Option[A]
  ): FormComponent[A]

object FieldBuilder:

  given (using fctx: FormBuilderContext): FieldBuilder[String] with
    override def required: Boolean = true
    override def build(
        fieldDescriptor: FieldDescriptor,
        initialValue: Option[String]
    ): FormComponent[String] =
      StringInputField(fieldDescriptor, initialValue)

  class StringInputField(
      desc: FieldDescriptor,
      initialValue: Option[String] = None
  )(using fctx: FormBuilderContext)
      extends FormComponent[String]:
    private val rawValue: Var[Option[String]] = Var(None)
    private val hadFocus: Var[Boolean] = Var(false)
    private val touched: Var[Boolean] = Var(false)

    override val validated: Signal[Validated[String]] = rawValue.signal.map {
      case Some(value) if value.trim.nonEmpty => Validation.succeed(value)
      case _ =>
        Validation.fail(UserMessage("error.value.required", desc.label))
    }

    override val elements: Seq[HtmlElement] =
      val hasError: Signal[Boolean] =
        validated.combineWithFn(touched.signal)((v, t) =>
          if t then v.fold(_ => true, _ => false) else false
        )

      val errors: Signal[List[UserMessage]] =
        validated.combineWithFn(touched.signal)((v, t) =>
          if t then v.fold(_.toList, _ => List.empty) else Nil
        )

      Seq(
        div(
          fctx.formUIFactory.input(hasError)(
            idAttr(desc.idString),
            nameAttr(desc.name),
            desc.placeholder.map(placeholder(_)),
            initialValue.map(L.value(_)),
            onInput.mapToValue.setAsValue --> rawValue.writer.contramap {
              (v: String) => Option(v).map(_.trim).filter(_.nonEmpty)
            },
            onFocus.mapTo(true) --> hadFocus.writer,
            onBlur.mapTo(true) --> touched.writer
          ),
          children <-- errors
            .map(
              _.map[HtmlElement](msg =>
                fctx.formUIFactory.validationError(
                  fctx.formMessagesResolver.message(msg)
                )
              )
            )
        )
      )

  given (using fctx: FormBuilderContext): FieldBuilder[Option[String]] with
    override def required: Boolean = false
    override def build(
        fieldDescriptor: FieldDescriptor,
        initialValue: Option[Option[String]]
    ): FormComponent[Option[String]] =
      OptionStringInputField(fieldDescriptor, initialValue)

  class OptionStringInputField(
      desc: FieldDescriptor,
      initialValue: Option[Option[String]] = None
  )(using fctx: FormBuilderContext)
      extends FormComponent[Option[String]]:
    private val rawValue: Var[Option[String]] = Var(None)
    private val hadFocus: Var[Boolean] = Var(false)
    private val touched: Var[Boolean] = Var(false)

    override val validated: Signal[Validated[Option[String]]] =
      rawValue.signal.map {
        case Some(value) if value.trim.nonEmpty =>
          Validation.succeed(Some(value))
        case _ => Validation.succeed(None)
      }

    override val elements: Seq[HtmlElement] =
      val hasError: Signal[Boolean] =
        validated.combineWithFn(touched.signal)((v, t) =>
          if t then v.fold(_ => true, _ => false) else false
        )

      val errors: Signal[List[UserMessage]] =
        validated.combineWithFn(touched.signal)((v, t) =>
          if t then v.fold(_.toList, _ => List.empty) else Nil
        )

      Seq(
        div(
          fctx.formUIFactory.input(hasError)(
            idAttr(desc.idString),
            nameAttr(desc.name),
            desc.placeholder.map(placeholder(_)),
            initialValue.flatten.map(L.value(_)),
            onInput.mapToValue.setAsValue --> rawValue.writer.contramap {
              (v: String) => Option(v).map(_.trim).filter(_.nonEmpty)
            },
            onFocus.mapTo(true) --> hadFocus.writer,
            onBlur.mapTo(true) --> touched.writer
          ),
          children <-- errors
            .map(
              _.map[HtmlElement](msg =>
                fctx.formUIFactory.validationError(
                  fctx.formMessagesResolver.message(msg)
                )
              )
            )
        )
      )
