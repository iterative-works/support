package works.iterative.ui.components.laminar.forms

import com.raquo.laminar.api.L.*
import works.iterative.core.*

case class InputField(
    desc: FieldDescriptor,
    initialValue: Option[String],
    validated: Signal[Validated[_]],
    observer: Observer[Option[String]]
)(using fctx: FormBuilderContext):
  val hadFocus: Var[Boolean] = Var(false)

  val touched: Var[Boolean] = Var(false)

  val hasError: Signal[Boolean] =
    validated.combineWithFn(touched.signal)((v, t) =>
      if t then v.fold(_ => true, _ => false) else false
    )

  val errors: Signal[List[UserMessage]] =
    validated.combineWithFn(touched.signal)((v, t) =>
      if t then v.fold(_.toList, _ => List.empty) else Nil
    )

  val elements: Seq[HtmlElement] =
    Seq(
      div(
        fctx.formUIFactory.input(hasError)(
          idAttr(desc.idString),
          nameAttr(desc.name),
          desc.placeholder.map(placeholder(_)),
          initialValue.map(value(_)),
          onInput.mapToValue.setAsValue --> observer.contramap { (v: String) =>
            Option(v).map(_.trim).filter(_.nonEmpty)
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

  val element: Div = div(elements*)
