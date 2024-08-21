package portaly
package forms
package impl

import com.raquo.laminar.api.L.*
import works.iterative.core.UserMessage
import org.scalajs.dom.MouseEvent
import works.iterative.ui.model.forms.{RelativePath, AbsolutePath, IdPath}

// Represents the internal state of the form
trait FormV:
    // Get the optional value signal of the given ID part
    def get(id: IdPath): Signal[Option[Any]]
    // Get the optional validation state of the given ID part
    def validation(id: IdPath): Signal[Option[ValidationState[Any]]]
    def all(f: IdPath => Boolean): Signal[List[Any]]
    // Get all the values under a certain prefix
    def under(id: IdPath): Signal[Map[IdPath, Any]]
end FormV

class FormVImpl extends FormV:
    private val values: Var[Map[IdPath, Any]] = Var(Map.empty)
    private val validations: Var[Map[IdPath, ValidationState[Any]]] = Var(Map.empty)

    override def get(id: IdPath): Signal[Option[Any]] =
        values.signal.map(_.get(id)).setDisplayName(s"state_values:${id.toHtmlId}")

    /** Collect all values matching the IdPath */
    override def all(f: IdPath => Boolean): Signal[List[Any]] =
        values.signal.map: v =>
            v.filter(v => f(v._1)).map(_._2).toList

    override def validation(id: IdPath): Signal[Option[ValidationState[Any]]] =
        validations.signal.map(_.get(id)).setDisplayName(s"state_validations:${id.toHtmlId}")

    override def under(id: IdPath): Signal[Map[IdPath, Any]] =
        values.signal.map(_.view.filterKeys(_.startsWith(id)).toMap)

    def updateValue(id: IdPath): Observer[Any] = values.updater((vals, v) => vals + (id -> v))
    def updateValidation(id: IdPath): Observer[ValidationState[Any]] =
        validations.updater((vals, v) => vals + (id -> v))
end FormVImpl

class FormCtx private (
    formState: FormVImpl,
    val path: IdPath,
    val displayAllErrors: Var[Boolean],
    buttonsBus: EventBus[IdPath],
    inputBus: EventBus[FormR],
    controlBus: EventBus[FormControl],
    val menuItems: List[AbsolutePath] = Nil,
    selectedItem: Var[Option[AbsolutePath]] = Var(None),
    val menuState: Map[AbsolutePath, Var[Option[Boolean]]] = Map.empty
):
    val level: Int = path.size
    val buttonClicks: EventStream[IdPath] = buttonsBus.events
    val buttonClicked: WriteBus[IdPath] = buttonsBus.writer

    val control: Observer[FormControl] = controlBus.writer
    val controlEvents: EventStream[FormControl] = controlBus.events

    val state: FormV = formState
    val showErrors: Signal[Boolean] = displayAllErrors.signal
    val updateValues: Observer[FormR] = inputBus.writer.setDisplayName("input bus")

    val inputValues: EventStream[FormR] = inputBus.events

    val currentItem: Signal[Option[AbsolutePath]] =
        selectedItem.signal

    val selectItem: Observer[Option[AbsolutePath]] =
        selectedItem.writer.contramap[Option[AbsolutePath]]:
            case Some(id) =>
                if menuItems.contains(id) then Some(id)
                else if menuItems.nonEmpty then Some(IdPath.FullPath(id.path.take(2)))
                else None
            case _ => None

    def selectNextItem(evt: MouseEvent): Unit =
        selectedItem.now().foreach: id =>
            val idx = menuItems.indexOf(id)
            if idx != -1 && idx < menuItems.size - 1 then
                evt.preventDefault()
                evt.stopPropagation()
                selectedItem.set(Some(menuItems(idx + 1)))
                Option(org.scalajs.dom.document.getElementsByTagName("h1").item(0)).foreach(
                    _.scrollIntoView()
                )
            end if
    end selectNextItem

    def selectPrevItem(evt: MouseEvent): Unit =
        selectedItem.now().foreach: id =>
            val idx = menuItems.indexOf(id)
            if idx != -1 && idx > 0 then
                evt.preventDefault()
                evt.stopPropagation()
                selectedItem.set(Some(menuItems(idx - 1)))
                Option(org.scalajs.dom.document.getElementsByTagName("h1").item(0)).foreach(
                    _.scrollIntoView()
                )
            end if
    end selectPrevItem

    val hasPrevItem: Signal[Boolean] = selectedItem.signal.map {
        case Some(item) => menuItems.indexOf(item) > 0
        case None       => false
    }

    // Lift a standalone field to work in the context
    def lift[RI, RO, A](
        id: RelativePath,
        rawInputFromGlobal: Option[List[Any]] => Option[RI],
        rawOutputToGlobal: RO => List[Any],
        outputToGlobal: A => List[Any]
    )(formPart: PartF[RI, RO, A]): Part =
        fi =>
            val innerId = fi.id / id
            val out = formPart(
                FormPartInputs(
                    innerId,
                    fi.formState,
                    fi.showErrors,
                    fi.rawInput.map(_.get(innerId)).collectOpt(rawInputFromGlobal),
                    fi.errorInput,
                    fi.control
                )
            )

            FormPartOutputs(
                out.id,
                out.rawOutput.map(v =>
                    impl.FormR.data(
                        works.iterative.ui.model.forms.IdPath.Root,
                        out.id.relativeTo(
                            works.iterative.ui.model.forms.IdPath.Root
                        ) -> rawOutputToGlobal(v)
                    )
                ),
                out.errorOutput,
                out.output.map(_.map(v =>
                    impl.FormR.data(
                        works.iterative.ui.model.forms.IdPath.Root,
                        out.id.relativeTo(
                            works.iterative.ui.model.forms.IdPath.Root
                        ) -> outputToGlobal(v)
                    )
                ) match
                    case ValidationState.Unknown(_) =>
                        ValidationState.Unknown(List(out.id -> UserMessage(
                            "error.unknown.validation",
                            out.id.toString()
                        )))
                    case v => v
                ),
                out.domOutput.amend(
                    out.rawOutput --> formState.updateValue(out.id),
                    out.output --> formState.updateValidation(out.id)
                )
            )
    end lift

    def liftStringInput[RO, A](id: RelativePath): PartF[String, RO, A] => Part =
        lift[String, RO, A](
            id,
            {
                case Some(v :: _) => v match
                        case str: String => Some(str)
                        case _           => None
                case _ => None
            },
            v => List(v),
            a => List(a)
        )

    def liftEmpty[RI, RO, A](id: RelativePath): PartF[RI, RO, A] => Part =
        lift[RI, RO, A](id, _ => None, _ => Nil, _ => Nil)
end FormCtx

object FormCtx:
    def apply(): FormCtx =
        new FormCtx(
            FormVImpl(),
            works.iterative.ui.model.forms.IdPath.Root,
            Var(false),
            new EventBus,
            new EventBus,
            new EventBus
        )

    def apply(menuItems: List[AbsolutePath]): FormCtx =
        new FormCtx(
            FormVImpl(),
            works.iterative.ui.model.forms.IdPath.Root,
            Var(false),
            new EventBus,
            new EventBus,
            new EventBus,
            menuItems,
            Var(menuItems.headOption),
            menuItems.map(_ -> Var(None)).toMap
        )

    def ctx(using context: FormCtx): FormCtx = context
end FormCtx
