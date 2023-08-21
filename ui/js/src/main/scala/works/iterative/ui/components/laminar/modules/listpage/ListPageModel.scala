package works.iterative.ui.components.laminar.modules.listpage

import works.iterative.core.UserMessage
import works.iterative.ui.model.Computable

trait ListPageModel[T]:

  enum Action:
    case SetItems(items: List[T])
    case VisitDetail(item: T)
    case SetError(message: UserMessage)

  enum Effect:
    case LoadItems
    case ReportError(message: UserMessage)
    case VisitDetail(item: T)

  case class Model(
      items: Computable[List[T]] = Computable.Uninitialized
  )

  trait Module extends works.iterative.ui.Module[Model, Action, Effect]:
    override def init: (Model, Option[Effect]) =
      Model() -> Some(Effect.LoadItems)

    override def handle(action: Action, model: Model): (Model, Option[Effect]) =
      action match
        case Action.SetItems(items) =>
          model.copy(items = model.items.update(items)) -> None
        case Action.SetError(msg) =>
          model -> Some(Effect.ReportError(msg))
        case Action.VisitDetail(item) =>
          model -> Some(Effect.VisitDetail(item))

    override def handleFailure: PartialFunction[Throwable, Option[Action]] = {
      case e: Throwable =>
        Some(
          Action.SetError(
            UserMessage("error.operation.failed", e.getMessage)
          )
        )
    }
