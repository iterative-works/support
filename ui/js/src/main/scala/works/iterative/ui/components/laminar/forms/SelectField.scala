package works.iterative.ui.components.laminar.forms

import com.raquo.laminar.api.L.*
import works.iterative.core.*
import works.iterative.ui.laminar.headless.Combobox

// TODO: this is a copy of InputField, but with a different logic for selects
// We need to merge the two after the shape is clear
case class SelectField(
    desc: FieldDescriptor,
    initialValue: Option[(String, String)],
    options: List[(String, String)],
    validated: Signal[Validated[_]],
    observer: Observer[String],
    combo: Boolean,
    add: Option[String => (String, String)]
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

  def makeField: HtmlElement =
    if !combo then
      val emptyValue = ("", "")
      val opts =
        if initialValue.isDefined || options.headOption.contains(
            emptyValue
          )
        then options
        else emptyValue :: options
      fctx.formUIFactory.select(hasError)(
        idAttr(desc.idString),
        nameAttr(desc.name),
        initialValue.map(i => value(i._1)),
        opts.map(o =>
          option(selected(initialValue.contains(o._1)), value(o._1), o._2)
        ),
        onChange.mapToValue.setAsValue --> observer,
        onFocus.mapTo(true) --> hadFocus.writer,
        onBlur.mapTo(true) --> touched.writer
      )
    else
      val addedOpt: Var[Option[(String, String)]] = Var(None)
      Combobox[(String, String)](initialValue)(
        fctx.formUIFactory.combobox
          .container(
            hasError,
            inp =>
              Combobox
                .input(_._2)(inp)
                .amend(
                  onFocus.mapTo(true) --> hadFocus.writer,
                  onBlur.mapTo(true) --> touched.writer
                )
          )(
            Combobox.button(fctx.formUIFactory.combobox.button()),
            Combobox.options(fctx.formUIFactory.combobox.options()) { v =>
              val ictx = summon[Combobox.ItemCtx[(String, String)]]
              fctx.formUIFactory.combobox
                .option(v._2, ictx.isActive, ictx.isSelected)()
            },
            Combobox.ctx.query.combineWithFn(addedOpt.signal) { (v, added) =>
              val search = v.toLowerCase()
              val opts = (added.toList ++ options)
                .filter(_._2.toLowerCase.contains(search))
              add match
                case Some(f) if opts.isEmpty && v.trim.nonEmpty =>
                  f(v) :: opts
                case _ => opts
            }
              --> Combobox.ctx.itemsWriter,
            Combobox.ctx.value.map(_.map(_._1).getOrElse("")) --> observer,
            Combobox.ctx.value.changes.filterNot(
              _.exists(v => options.exists(o => o._1 == v._1))
            ) --> addedOpt.writer
          )
      )

  val elements: Seq[HtmlElement] =
    Seq(
      div(
        makeField,
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
