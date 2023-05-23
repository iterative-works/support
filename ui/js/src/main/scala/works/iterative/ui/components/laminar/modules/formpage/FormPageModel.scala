package works.iterative.ui.components.laminar.modules.formpage

import works.iterative.ui.model.Computable
import works.iterative.core.UserMessage

trait FormPageModel[T]:

  enum Action:
    case SetInitialValue(value: Option[T])
    case Submit(value: T)
    case Submitted
    case SetError(message: UserMessage)
    case Cancel

  enum Effect:
    case LoadInitialValue
    case Submit(value: T)
    case ReportError(message: UserMessage)
    case Cancel

  case class Model(
      initialValue: Computable[Option[T]],
      submitted: Boolean = false
  )

  trait Module extends works.iterative.ui.Module[Model, Action, Effect]:
    override def init: (Model, Option[Effect]) =
      Model(Computable.Uninitialized) -> Some(Effect.LoadInitialValue)

    override def handle(action: Action, model: Model): (Model, Option[Effect]) =
      action match
        case Action.SetInitialValue(value) =>
          model.copy(initialValue = model.initialValue.update(value)) -> None
        case Action.Submit(value) =>
          model -> Some(Effect.Submit(value))
        case Action.Cancel =>
          model -> Some(Effect.Cancel)
        case Action.SetError(msg) =>
          model -> Some(Effect.ReportError(msg))
        case Action.Submitted =>
          model.copy(submitted = true) -> None

    override def handleFailure: PartialFunction[Throwable, Option[Action]] = {
      case e: Throwable =>
        Some(
          Action.SetError(
            UserMessage("error.operation.failed", e.getMessage)
          )
        )
    }
