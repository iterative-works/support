package portaly.forms
package impl

import com.raquo.laminar.api.L.*
import io.laminext.syntax.core.*
import works.iterative.core.MessageCatalogue
import works.iterative.ui.components.laminar.LaminarExtensions.*
import FormCtx.ctx
import com.raquo.laminar.api.L
import portaly.forms.Components.RadioOption
import works.iterative.core.UserMessage
import zio.prelude.*
import org.scalajs.dom
import zio.NonEmptyChunk
import works.iterative.ui.model.forms.{RelativePath, AbsolutePath}
import works.iterative.core.MessageId
import works.iterative.core.Language
import org.scalajs.dom.Event
import org.scalajs.dom.EventInit

class LiveHtmlInterpreter(
    layoutResolver: LayoutResolver,
    fieldTypeResolver: FieldTypeResolver,
    validationResolver: ValidationResolver,
    displayResolver: LiveHtmlDisplayResolver,
    buttonHandler: ButtonHandler,
    persistenceProvider: PersistenceProvider,
    hooks: LiveFormHooks,
    override val cs: Components,
    menuItems: Form => List[AbsolutePath] = _ => Nil,
    private val aroundTitle: HtmlElement => HtmlElement = identity,
    private val formMods: Option[HtmlMod] = None
)(using messages: MessageCatalogue, lang: Language) extends HtmlInterpreter:
    given Components = cs

    override def aroundTitle(f: HtmlElement => HtmlElement): HtmlInterpreter =
        new LiveHtmlInterpreter(
            layoutResolver,
            fieldTypeResolver,
            validationResolver,
            displayResolver,
            buttonHandler,
            persistenceProvider,
            hooks,
            cs,
            menuItems,
            f
        )

    override def withFormMods(mods: HtmlMod): HtmlInterpreter =
        new LiveHtmlInterpreter(
            layoutResolver,
            fieldTypeResolver,
            validationResolver,
            displayResolver,
            buttonHandler,
            persistenceProvider,
            hooks,
            cs,
            menuItems,
            aroundTitle,
            Some(mods)
        )

    def withComponents(components: Components): HtmlInterpreter =
        new LiveHtmlInterpreter(
            layoutResolver,
            fieldTypeResolver,
            validationResolver,
            displayResolver,
            buttonHandler,
            persistenceProvider,
            hooks,
            components,
            menuItems,
            aroundTitle,
            formMods
        )

    def withAutocompleteContext(context: Map[String, String]): LiveHtmlInterpreter =
        new LiveHtmlInterpreter(
            layoutResolver,
            fieldTypeResolver.withAutocompleteContext(context),
            validationResolver,
            displayResolver,
            buttonHandler,
            persistenceProvider,
            hooks,
            cs,
            menuItems,
            aroundTitle,
            formMods
        )

    override def interpret(
        id: FormIdent,
        form: Form,
        formData: Option[FormR]
    ): LiveForm =
        given ctx: FormCtx = FormCtx(menuItems(form))
        LiveFormImpl(
            id,
            form,
            formData,
            persistenceProvider,
            render(form)
        )
    end interpret

    private def render(form: Form)(using
        ctx: FormCtx
    ): Render[FormPartOutputs[FormR, List[UserMessage], ValidationState[FormR]]] =
        val rendered = renderSegment(form)
        rendered(FormPartInputs(
            works.iterative.ui.model.forms.IdPath.Root,
            ctx.state,
            ctx.showErrors,
            ctx.inputValues.flatMapSwitch(single =>
                val itemsFirst = single.filterKeys(_.last == "__items")
                val restLater = single.filterKeys(_.last != "__items")
                EventStream.fromSeq(Seq(itemsFirst, restLater))
            ).setDisplayName("input events"),
            EventStream.empty,
            ctx.controlEvents
        ))
    end render

    private def renderSegment(element: FormSegment, repeatIndex: Option[Int] = None): RenderPart =
        element match
            case Form(id, _, sections) => renderForm(id, sections)
            case Section(id, elems, sectionType) =>
                renderSection(id, sectionType, elems, repeatIndex)
            case Field(id, fieldType, default, optional) =>
                renderFormField(id, fieldType, default, optional)
            case File(id, multiple, optional) => renderFileField(id, multiple, optional)
            case Date(id)                     => renderTextField(id, "date", None)
            case Display(id)                  => renderDisplay(id)
            case Button(id)                   => renderButton(id)
            case Enum(id, values, default) =>
                if values.size == 2 && values.contains("true") && values.contains(
                        "false"
                    )
                then renderCheckbox(id, default)
                else renderEnum(id, values, default, required = true)
            case ShowIf(condition, elem) => renderShowIf(condition, elem)
            case Repeated(id, default, optional, elems) =>
                renderRepeated(id, default, optional, elems)

    private def renderForm(id: RelativePath, sections: Seq[SectionSegment]): RenderPart =
        def cont(i: SectionSegment) = renderSegment(i)
        val fields = sections.map(cont)
        FormPart.combine(id, fields*): content =>
            cs.form(
                id.toHtmlId,
                aroundTitle(cs.formTitle(id.toMessageNode("title"))),
                formMods,
                content
            )
    end renderForm

    private def renderRepeated(
        id: RelativePath,
        default: Option[(String, String)],
        optional: Boolean,
        elems: List[SectionSegment]
    ): RenderPart =
        val inner =
            val elemList = elems.map(e => e.id.last -> e)
            val elemMap = elemList.toMap
            (i: String, idx: Int) => renderSegment(elemMap.getOrElse(i, elems.head), Some(idx))
        end inner
        // Create a string from 6 chars from a..z
        def shortRandomId = scala.util.Random.alphanumeric.take(6).mkString
        fi =>
            // We need to keep a snapshot of the input values
            // because we might be creating new segments from data
            // and the segments would not receive the original data after creation
            val snapshot: Var[FormR] = Var(FormR.empty)

            // A current list of segments we have
            val items: Var[List[(String, String)]] = Var(default.toList)

            // We take the last snapshot, when it arrives, and create the segments
            val createSegments: HtmlMod =
                snapshot.signal.changes.map(_.get(fi.id / id / "__items")).collect {
                    case Some(it) => it.collect {
                            case v: String => v
                        }.map(_.split(":")).collect {
                            case Array(k, v) => k -> v
                        }
                } --> items.writer

            val innerOutputs =
                items.signal.map(_.zipWithIndex).split(_._1._1)((key, init, updates) =>
                    val ((_, elemId), idx) = init
                    inner(elemId, idx)(fi.mapId(_ / id / key).composeRawInput(
                        // Init the segment with the data from the snapshot, after emit the input data
                        ri => EventStream.merge(EventStream.unit().sample(snapshot.signal), ri)
                    )).mapDom: elem =>
                        elem.amend(
                            idAttr((fi.id / id / key / elemId).toHtmlId),
                            cls("relative"),
                            span(
                                cls("text-xs absolute right-0 top-0 flex"),
                                /*
                                div(
                                    cls("inline-block mt-1 mr-1"),
                                    child.text <-- updates.map(_._2 + 1)
                                ),
                                 */
                                span(
                                    cls(
                                        "bg-red-700 text-red-100 text-sm flex items-center button hover:bg-red-600"
                                    ),
                                    span("Odebrat "),
                                    cs.segmentRemoveIcon(svg.cls("w-4 h-4 cursor-pointer")),
                                    onClick.mapTo(key) --> items.updater((its, it) =>
                                        its.filterNot(_._1 == it)
                                    )
                                )
                            )
                        )
                )

            val itemList =
                items.signal.map(i =>
                    FormR.data(
                        fi.id,
                        (id / "__items") -> i.map(v =>
                            s"${v._1}:${v._2}"
                        )
                    )
                )

            val idPath = fi.id / id

            val ownErrors = if optional then Val(Nil)
            else
                items.signal.map(i =>
                    if i.size > 0 then Nil
                    else List(UserMessage("error.value.required", idPath.toMessage("section")))
                )

            FormPartOutputs(
                idPath,
                innerOutputs.flatMapSwitch(i =>
                    Signal.combineSeq(i.map(_.rawOutput)).map(_.reduceIdentity).combineWithFn(
                        itemList
                    )(_ <> _)
                ),
                innerOutputs.changes.flatMapSwitch(i =>
                    EventStream.combineSeq(i.map(_.errorOutput)).map(
                        _.fold(Nil)(_ ++ _)
                    ).combineWithFn(ownErrors.changes)(_ ++ _)
                ),
                innerOutputs.flatMapSwitch(i =>
                    Signal.combineSeq(i.map(_.output)).map(_.reduceIdentity).combineWithFn(
                        ownErrors,
                        itemList
                    )((v, e, i) =>
                        val own = e match
                            case Nil => ValidationState.Valid(i)
                            case h :: hs => ValidationState.Invalid(NonEmptyChunk(
                                    (idPath, h),
                                    hs.map((idPath, _))*
                                ))
                        v <> own
                    )
                ),
                div(
                    idAttr(idPath.toHtmlId),
                    input(
                        tpe("hidden"),
                        idAttr((idPath / "__items").toHtmlId),
                        nameAttr(
                            (idPath / "__items").toHtmlName
                        ),
                        value <-- items.signal.map(_.map((a, b) => s"$a:$b").mkString(",")),
                        inContext { el =>
                            items.signal.changes --> (items =>
                                val _ = el.ref.dispatchEvent(new Event(
                                    "itemsChanged",
                                    new EventInit:
                                        bubbles = true
                                ))
                            )
                        }
                    ),
                    children <-- innerOutputs.map(_.map(_.domOutput)),
                    fi.rawInput --> snapshot.writer,
                    createSegments,
                    children <-- ownErrors.map:
                        _.map[HtmlElement]: msg =>
                            p(cls("mt-2 text-sm text-red-800"), msg.asElement)
                    ,
                    div(
                        cls("grid grid-cols-2 gap-2 mt-2 pb-2"),
                        idPath.toMessageNodeOpt("add.label").map(n =>
                            h4(cls("col-span-2 text-base font-medium"), n)
                        ),
                        elems.map: elem =>
                            val vId = idPath / elem.id / "add"
                            cs.button(
                                vId.toHtmlId,
                                vId.toHtmlName,
                                vId.toMessage("button"),
                                "button",
                                mods = cls("mt-2"),
                                onClick.mapTo(shortRandomId) --> items.updater[String]((c, a) =>
                                    c :+ (a -> elem.id.last)
                                )
                            )
                    )
                )
            )
    end renderRepeated

    private def renderShowIf(condition: Condition, elem: SectionSegment): RenderPart =
        val innerPart = renderSegment(elem)

        fi =>
            val snapshot: Var[FormR] = Var(FormR.empty)
            val resolved: Signal[Boolean] = resolveCondition(condition)(fi.id, fi.formState)
            val inner =
                innerPart(fi.composeRawInput(_ => snapshot.signal.changes))

            FormPartOutputs(
                inner.id,
                inner.rawOutput.combineWithFn(resolved) {
                    case (o, true) => o
                    case _         => FormR.empty
                },
                inner.errorOutput.withCurrentValueOf(resolved).map({
                    case (o, true) => o
                    case _         => Nil
                }),
                inner.output.combineWithFn(resolved) {
                    case (o, true) => o
                    case _         => ValidationState.Valid(FormR.empty)
                },
                div(
                    resolved.childWhenTrue(inner.domOutput),
                    fi.rawInput.filter(_.nonEmpty) --> snapshot.writer
                )
            )
    end renderShowIf

    private def renderSection(
        id: RelativePath,
        sectionType: String,
        elems: List[SectionSegment],
        repeatIndex: Option[Int]
    ): RenderPart =
        def cont(i: SectionSegment) = renderSegment(i)
        hooks.aroundSection(id): fi =>
            val fullId = fi.id / id
            val layout = layoutResolver.resolve(fullId, elems)
            val content = layout match
                case Grid(elems) =>
                    FormPart.combine(
                        id,
                        elems.flatMap(segments =>
                            segments.map(cont(_).mapDom(cs.gridCell(segments.size, _)))
                        )*
                    )(cs.grid)
                case Flex(elems) =>
                    FormPart.combine(id, elems.map(cont)*)(cs.flexRow)

            val unknown = ValidationState.Unknown(
                List(fi.id -> UserMessage("error.validation.unknown", fi.id.toHtmlId))
            )
            val validationResult = Var[ValidationState[FormR]](unknown)

            val out = content(fi)

            def mods(e: HtmlElement) = if sectionType != "any" then
                val validation = validationResolver.resolveSection(sectionType)
                val validationRule = validation(fi.id / id)
                val (validations, validationWriter) =
                    EventStream.withObserver[ValidationState[FormR]]

                modSeq(
                    e,
                    out.output.changes.throttle(500, false).setDisplayName(
                        "to_validate"
                    ) --> validationWriter,
                    EventStream.unit().sample(out.output) --> validationWriter,
                    validations.flatMapSwitch {
                        case ValidationState.Valid(v) => validationRule(v)
                        case _                        => EventStream.fromValue(unknown)
                    } --> validationResult.writer
                )
            else modSeq(e)

            val errors = validationResult.signal.map {
                case ValidationState.Invalid(errs) => errs.toList.map(_._2)
                case _                             => Nil
            }

            val output = if sectionType == "any" then out.output
            else out.output.combineWithFn(validationResult.signal)(_ <> _)

            val menuState = FormCtx.ctx.menuState.get(fullId).getOrElse(Var(None))

            def sectionTitle =
                (fullId.toMessageIds("section") match
                    case Vector[MessageId](h) =>
                        messages.get(UserMessage(h, repeatIndex.map(_ + 1).toList*))
                    case h +: hs => messages.opt(
                            UserMessage(h, repeatIndex.map(_ + 1).toList*),
                            hs.map(h => UserMessage(h, repeatIndex.map(_ + 1).toList*))*
                        )
                ).map(i =>
                    span(dataAttr("msgId")(s"${fullId.toHtmlName}.section"), i)
                )

            FormPartOutputs(
                out.id,
                out.rawOutput,
                if sectionType == "any" then out.errorOutput
                else out.errorOutput.combineWithFn(errors.changes)(_ ++ _),
                output,
                cs.section(
                    fullId.toHtmlId,
                    fullId.size,
                    sectionTitle,
                    fullId.toMessageNodeOpt("section.subtitle"),
                    fi.showErrors.combineWithFn(errors): (show, errs) =>
                        if show then errs.map(_.asElement) else Nil,
                    mods(out.domOutput),
                    if fullId.size == 2 && ctx.menuItems.nonEmpty then
                        modSeq(
                            cls("hidden") <-- FormCtx.ctx.currentItem.map(
                                _.forall(_ == fullId)
                            ).not,
                            output.map(_.isValid).map(Some(_)) --> menuState.writer
                        )
                    else emptyMod
                )
            )
    end renderSection

    private def renderFormField(
        id: RelativePath,
        fieldType: FieldType,
        default: Option[String],
        optional: Boolean
    ): RenderPart =
        ctx.liftStringInput(id)(fieldTypeResolver
            .resolve(fieldType)
            .render(id, !optional, default))

    private def renderFileField(
        id: RelativePath,
        multiple: Boolean,
        optional: Boolean
    ): RenderPart =
        val req: SRule[List[org.scalajs.dom.File]] =
            if optional then ValidationRule.valid
            else
                ValidationRule((a: List[org.scalajs.dom.File]) =>
                    if a.isEmpty then
                        ValidationState.Invalid(
                            id,
                            UserMessage("error.field.required", id.toMessage("label"))
                        ).succeed
                    else ValidationState.Valid(a).succeed
                )
        ctx.lift[List[String], List[org.scalajs.dom.File], List[org.scalajs.dom.File]](
            id,
            {
                case Some(v) =>
                    val names = v.collect:
                        case x: String => x
                    if names.nonEmpty then Some(names) else None
                case _ => None
            },
            identity,
            identity
        )(
            ValidatingFormField(req)(
                LabeledFormField(FileFormField(multiple), Val(!optional)),
                Val(false)
            )
        )
    end renderFileField

    private def renderTextField(
        id: RelativePath,
        inputType: String,
        default: Option[String]
    ): RenderPart =
        ctx.liftStringInput(id) {
            val field = TextFormField(inputType, default, true, None)
            ValidatingFormField(ValidationRule.valid)(
                LabeledFormField(field, Val(false)),
                field.touched
            )
        }

    private def resolveCondition(condition: Condition)(
        baseId: AbsolutePath,
        state: FormV
    ): Signal[Boolean] =
        import Condition.*
        condition match
            case Never              => Val(false)
            case Always             => Val(true)
            case AnyOf(conditions*) => resolveConditionsOr(conditions)(baseId, state)
            case AllOf(conditions*) => resolveConditions(conditions)(baseId, state)
            case IsEqual(idp, value) => state.get(works.iterative.ui.model.forms.IdPath.parse(
                    idp,
                    baseId
                )).map(_.contains(value))
            case IsValid(idp) =>
                state.validation(works.iterative.ui.model.forms.IdPath.parse(idp, baseId)).map(
                    _.exists(_.isValid)
                )
            case NonEmpty(idp) =>
                state.get(works.iterative.ui.model.forms.IdPath.parse(idp, baseId)).map(
                    _.filterNot {
                        case s: String => s.isBlank
                        case _         => false
                    }.nonEmpty
                )
        end match
    end resolveCondition

    private def resolveConditionsOr(conditions: Seq[Condition])(
        baseId: AbsolutePath,
        state: FormV
    ): Signal[Boolean] =
        conditions.map(resolveCondition(_)(baseId, state)).reduce(_ || _)

    private def resolveConditions(conditions: Seq[Condition])(
        baseId: AbsolutePath,
        state: FormV
    ): Signal[Boolean] =
        conditions.map(resolveCondition(_)(baseId, state)).reduce(_ && _)

    private def renderButton(id: RelativePath): RenderPart =
        ctx.liftEmpty(id)(FormPart.domOnly(fi =>
            cs.button(
                fi.id.toHtmlId,
                fi.id.toHtmlName,
                fi.id.toMessage("button"),
                "button",
                buttonHandler.register(fi.id)
            )
        ).valid)
    end renderButton

    private def renderCheckbox(id: RelativePath, default: Option[String]): RenderPart =
        ctx.liftStringInput(id) {
            FormPart.fromVar[String](id, Var(default.getOrElse("false")))(inputVal =>
                fi =>
                    cs.checkbox(
                        fi.id.toHtmlId,
                        fi.id.toHtmlName,
                        inputVal.signal,
                        fi.id.toMessageNode("label"),
                        fi.id.toMessageNodeOpt("label.description"),
                        onClick.mapTo(inputVal.now()).map {
                            case "true" => "false"
                            case _      => "true"
                        } --> inputVal.writer,
                        EventStream.fromSeq(default.toSeq) --> inputVal.writer
                    )
            )
        }

    private def renderEnum(
        id: RelativePath,
        values: List[String],
        default: Option[String],
        required: Boolean
    ): RenderPart =

        type RenderF = Var[String] => FormPartInputs[Any, String, Nothing] => HtmlElement

        val renderRadio: RenderF = inputVal =>
            fi =>
                cs.radio(
                    fi.id.toHtmlId,
                    fi.id.toHtmlName,
                    fi.id.toMessageNode("label"),
                    fi.id.toMessageNodeOpt("description"),
                    Val(required),
                    inputVal.signal,
                    values.map { v =>
                        val vId = fi.id / v
                        RadioOption(
                            vId.toHtmlId,
                            v,
                            vId.toMessageNode("label"),
                            vId.toMessageNodeOpt("help"),
                            onClick.mapTo(v) --> inputVal.writer.setDisplayName(
                                s"set_enum:${id}"
                            )
                        )
                    },
                    fi.showErrors.combineWithFn(inputVal.signal): (se, value) =>
                        se && value.isEmpty && required,
                    EventStream.fromSeq(default.toSeq) --> inputVal.writer
                )

        val renderSelect: RenderF = inputVal =>
            fi =>
                cs.select(
                    fi.id.toHtmlId,
                    fi.id.toHtmlName,
                    fi.id.toMessageNode("label"),
                    fi.id.toMessageNodeOpt("description"),
                    Val(required),
                    inputVal.signal,
                    values.map { v =>
                        val vId = fi.id / v
                        RadioOption(
                            vId.toHtmlId,
                            v,
                            vId.toMessageNode("label"),
                            vId.toMessageNodeOpt("help")
                        )
                    },
                    EventStream.fromSeq(default.toSeq) --> inputVal.writer,
                    onChange.mapToValue --> inputVal.writer
                )

        val renderF: RenderF = if values.length > 3 then renderSelect else renderRadio

        def validateEnum(a: String): ValidationState[String] = a match
            case a if a.isBlank && required =>
                ValidationState.Invalid(
                    id,
                    UserMessage("error.field.required", id.toMessage("label"))
                )
            case a => ValidationState.Valid(a)

        val buildField =
            FormPart.fromVar[String](
                id,
                Var(default.getOrElse("")),
                validateEnum
            )(renderF)

        ctx.liftStringInput(id)(buildField)
    end renderEnum

    private def renderDisplay(id: RelativePath): RenderPart =
        ctx.liftEmpty(id)(
            FormPart.domOnly(fi => displayResolver.resolve(fi.id, FormCtx.ctx)).valid
        )
end LiveHtmlInterpreter

object LiveHtmlInterpreter:
    def firstLevelMenuItems(form: Form): List[AbsolutePath] =
        form.elems.collect {
            case Section(id, _, _) => works.iterative.ui.model.forms.IdPath.Root / form.id / id
        }
end LiveHtmlInterpreter
