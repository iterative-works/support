package works.iterative.ui.components.laminar.forms

import com.raquo.laminar.api.L.*
import works.iterative.core.*
import works.iterative.ui.laminar.headless.Combobox

// TODO: this is a copy of InputField, but with a different logic for selects
// We need to merge the two after the shape is clear
case class SelectField[A](
    desc: FieldDescriptor,
    id: A => String,
    label: A => String,
    initialValue: Option[A],
    initialOptions: List[A],
    validated: Signal[Validated[?]],
    observer: Observer[Option[A]],
    combo: Boolean,
    add: Option[String => Validated[A]],
    queryOptions: Option[String => EventStream[List[A]]]
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

    val initialOptionsStream = EventStream.fromValue(initialOptions)

    def findOptions(
        v: String,
        selectedOpt: Option[A]
    ): EventStream[List[A]] =
        def matches(o: A) = label(o).toLowerCase.contains(v.toLowerCase)

        def withAddOption(opts: List[A]) =
            add match
                case Some(f) if opts.isEmpty && v.trim.nonEmpty =>
                    f(v).toOption.toList ++ opts
                case _ => opts

        def withSelected(opts: List[A]) =
            val matchedSelected = selectedOpt.filter(matches)
            matchedSelected match
                case Some(s) if !opts.contains(s) => s :: opts
                case _                            => opts
        end withSelected

        queryOptions match
            case Some(query) =>
                query(v).map(opts => withSelected(withAddOption(opts)))
            case None =>
                EventStream.fromValue(
                    withSelected(withAddOption(initialOptions.filter(matches)))
                )
        end match
    end findOptions

    def makeField: HtmlElement =
        if !combo then
            val opts = None :: initialOptions.map(Some(_))
            val optMap = initialOptions.map(o => id(o) -> o).toMap
            fctx.formUIFactory.select(hasError)(
                idAttr(desc.idString),
                nameAttr(desc.name),
                opts.map(o =>
                    option(
                        defaultSelected(initialValue == o),
                        value(o.map(id).getOrElse("")),
                        o.map(label).getOrElse("")
                    )
                ),
                onChange.mapToValue.setAsValue.map(optMap.get) --> observer,
                onFocus.mapTo(true) --> hadFocus.writer,
                onBlur.mapTo(true) --> touched.writer
            )
        else
            Combobox[A](initialValue)(
                fctx.formUIFactory.combobox
                    .container(
                        hasError,
                        inp =>
                            Combobox
                                .input(label)(inp)
                                .amend(
                                    onFocus.mapTo(true) --> hadFocus.writer,
                                    onBlur.mapTo(true) --> touched.writer
                                )
                    )(
                        Combobox.button(fctx.formUIFactory.combobox.button()),
                        Combobox.options(fctx.formUIFactory.combobox.options()) { v =>
                            val ictx = summon[Combobox.ItemCtx[A]]
                            fctx.formUIFactory.combobox
                                .option(label(v), ictx.isActive, ictx.isSelected)()
                        },
                        Combobox.ctx.query
                            .withCurrentValueOf(Combobox.ctx.value)
                            .flatMapSwitch(findOptions) --> Combobox.ctx.itemsWriter,
                        Combobox.ctx.value --> observer
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
end SelectField
