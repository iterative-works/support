package works.iterative.ui.model

import works.iterative.core.*
import zio.prelude.*

trait HtmlUIBuilder[Node, Context, Sub[+_]]:
    type Ctx = Context
    type Output = Node

    // TODO: remove the Context type parameter, it will not be used as soon as we refactor the MessageCatalogue
    type Render[+A] = Ctx ?=> A

    type Rendered = Render[Output]

    type RenderBlock = Render[Block]

    class UIConfig(values: (String, Any)*) extends Selectable:
        private val config = values.toMap
        def selectDynamic(name: String): Any = config.get(name)

        def set(key: String, value: Any): UIConfig =
            UIConfig(config.updated(key, value).toSeq*)
    end UIConfig

    sealed trait UIElement

    final case class Blocks(
        id: String,
        title: Option[Block.Title],
        items: Vector[Block]
    ) extends UIElement

    object Blocks:
        def apply(id: String, items: Block*): Blocks =
            Blocks(id, None, items.toVector)

    final case class Block(
        id: Block.Id,
        title: Block.Title,
        subtitle: Block.Subtitle,
        status: Block.Status,
        actions: Block.Actions,
        content: Block.Content,
        filter: Block.Filter,
        footer: Block.Footer
    ) extends UIElement

    object Block:
        type Id = String
        type Title = Output
        type Subtitle = Option[Output]
        type Content = Reader[Any, Output]
        type Footer = Option[Output]
        type Status = Vector[Output]
        type Filter = Option[Output]

        enum Actions:
            case Direct(actions: List[Action])
            case Deferred(actions: Sub[List[Action]])

        object Actions:
            given Conversion[List[Action], Actions] = Direct(_)
            given Conversion[Sub[List[Action]], Actions] = Deferred(_)
    end Block

    trait UIInterpreter:
        def render(el: UIElement): Rendered
        def withConfig(config: UIConfig): UIInterpreter
end HtmlUIBuilder
