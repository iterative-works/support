package works.iterative.ui.components

trait ActionComponents[T]:
    import ActionComponents.*

    def button(item: ActionItemView): T

    def dropdown(name: String, items: Seq[ActionItemView]): T
end ActionComponents

object ActionComponents:
    final case class ActionItemView(
        name: String,
        title: String,
        variant: String,
        handler: ActionItemHandler = ActionItemHandler.NoAction,
        icon: Option[String] = None
    )

    enum ActionItemHandler:
        case Href(url: String)
        case Handler(handler: () => Unit)
        case Attrs(attrMap: Map[String, String])
        case Id(id: String)
        case NoAction
    end ActionItemHandler
end ActionComponents
