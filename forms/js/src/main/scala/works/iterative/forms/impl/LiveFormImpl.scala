package portaly.forms
package impl

import com.raquo.laminar.api.L.*
import io.laminext.syntax.core.*
import zio.json.*
import scala.scalajs.js
import org.scalajs.dom
import works.iterative.core.UserMessage
import works.iterative.core.MessageCatalogue
import com.raquo.airstream.core.Observer
import works.iterative.ui.model.forms.{AbsolutePath, IdPath}

class LiveFormImpl(
    val id: FormIdent,
    form: Form,
    initialData: Option[FormR],
    persistenceProvider: PersistenceProvider,
    rendered: FormPartOutputs[FormR, List[UserMessage], ValidationState[FormR]],
    doneHook: Option[FormR => FormR] = None
)(using ctx: FormCtx, messages: MessageCatalogue, cs: Components) extends LiveForm:
    override val rawData: Signal[FormR] = rendered.rawOutput
    override val rawInput: Observer[FormR] = ctx.updateValues
    override val data: Signal[ValidationState[FormR]] = rendered.output
    override val buttonClicks: EventStream[IdPath] = ctx.buttonClicks
    override def control: Observer[FormControl] = ctx.control
    override def aroundDone(advice: FormR => FormR): LiveForm =
        new LiveFormImpl(id, form, initialData, persistenceProvider, rendered, Some(advice))
    override def aroundDone(advice: Option[FormR => FormR]): LiveForm =
        new LiveFormImpl(id, form, initialData, persistenceProvider, rendered, advice)

    private val (resetEvents, doReset) = EventStream.withObserver[Unit]

    override val element = cs.formLayout(
        rendered.domOutput.amend(
            EventStream.fromSeq(initialData.toSeq).delay(0) --> ctx.updateValues,
            // TODO: backpressure from persistence provider
            persistenceProvider.reader(
                id,
                form.id.last,
                form.version
            ).delay(0).collectSome --> ctx.updateValues,
            rendered.rawOutput.changes.throttle(
                2000,
                leading = false
            ).map(PersistenceData.Draft(_)) --> persistenceProvider.writer(
                id,
                form.id.last,
                form.version
            ),
            resetEvents.mapTo(PersistenceData.Reset) --> persistenceProvider.writer(
                id,
                form.id.last,
                form.version
            )
        ),
        renderMenu
    )

    override val reset: Observer[Unit] = doReset

    override def resetButton: HtmlElement = cs.button(
        "form-reset",
        "form.reset",
        messages("reset.button"),
        "reset",
        onClick.mapToUnit --> doReset,
        cls("hidden") <-- ctx.hasPrevItem.not
    )

    override def saveButton: HtmlElement = a(
        cs.buttonLike,
        cls("hidden") <-- ctx.hasPrevItem.not,
        messages("save.button"),
        download(s"${form.id.last}-${form.version}.json"),
        href <-- rawData.changes.throttle(5000, false).map(_.toJson).map: json =>
            val blob =
                new dom.Blob(
                    js.Array(json),
                    new dom.BlobPropertyBag:
                        `type` = "application/json"
                )
            dom.URL.createObjectURL(blob)
    )

    override def doneButton(onDone: Observer[FormR]): HtmlElement = cs.button(
        "form-done",
        "form.done",
        messages("done.button"),
        "submit",
        disabled <-- data.signal.map(_.isValid).not,
        onClick.compose(
            _.sample(data)
                .collect:
                    case ValidationState.Valid(d) => d
                .map: d =>
                    doneHook.fold(d)(_(d))
                .map:
                    PersistenceData.Done(_)
        ) --> Observer.combine[PersistenceData](
            persistenceProvider.writer(
                id,
                form.id.last,
                form.version
            ),
            onDone.contracollect {
                case PersistenceData.Done(d) => d
            }
        )
    )

    override def importButton: HtmlElement =
        cs.fileInput(
            "import-button",
            "import.button",
            multiple = false,
            messages("import.button"),
            modSeq(
                inContext[Input](thisNode =>
                    onInput.mapTo(thisNode.ref.files) --> { files =>
                        if files.length > 0 then
                            val file = files(0)
                            val reader = new dom.FileReader()
                            reader.readAsText(file)
                            reader.onload = _ =>
                                val content = reader.result.asInstanceOf[String]
                                content.fromJson.foreach(ctx.updateValues.onNext)
                    }
                ),
                accept("application/json")
            )
        ).amend(cls("hidden") <-- ctx.hasPrevItem)

    override def nextButton: HtmlElement = cs.button(
        "form-next",
        "form.next",
        messages("next.button"),
        "submit",
        onClick --> ctx.selectNextItem
    )

    override def previousButton: HtmlElement = cs.button(
        "form-previous",
        "form.previous",
        messages("previous.button"),
        "button",
        onClick --> ctx.selectPrevItem,
        cls("hidden") <-- ctx.hasPrevItem.not
    )

    override def switchTo: Observer[AbsolutePath] = ctx.selectItem.contramapSome

    override def showErrors: Observer[Boolean] = ctx.displayAllErrors.writer

    private def renderMenu: Render[HtmlElement] =
        div(
            cls(
                "flex justify-between grow flex-col gap-y-5 overflow-y-auto bg-gray-50 px-6 py-4 text-sm shadow-lg "
            ),
            ul(
                FormCtx.ctx.menuItems.map: id =>
                    val state: Var[Option[Boolean]] =
                        FormCtx.ctx.menuState.get(id).getOrElse(Var(None))
                    li(a(
                        href("#"),
                        cls("font-medium text-[#6281C6]") <-- ctx.currentItem.map(
                            _.contains(id)
                        ),
                        onClick.preventDefault.mapTo(Some(id)) --> ctx.selectItem,
                        id.toMessageNode("menu"),
                        child.maybe <-- state.signal.map {
                            case Some(false) => Some(span(cls("text-red-600"), " ✗"))
                            case Some(true)  => Some(span(cls("text-green-600"), " ✓"))
                            case _           => None
                        }
                    ))
            ),
            ul(
                cls("flex flex-col gap-y-2"),
                li(saveButton.amend(cls("w-full"))),
                li(importButton.amend(cls("w-full"))),
                li(resetButton.amend(cls("w-full")))
            )
        )
end LiveFormImpl
