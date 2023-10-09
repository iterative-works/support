package works.iterative.ui.model

import works.iterative.core.*
import zio.prelude.*

trait HtmlUIBuilder[Node, Context]:
  type Ctx = Context
  type Output = Node
  type Rendered = Ctx ?=> Output

  sealed trait UIElement

  case class Block(
      id: Block.Id,
      title: Block.Title,
      subtitle: Block.Subtitle,
      status: Block.Status,
      actions: Block.Actions,
      content: Block.Content,
      footer: Block.Footer,
  ) extends UIElement

  object Block:
    type Id = String
    type Title = Output
    type Subtitle = Option[Output]
    type Actions = List[Action]
    type Content = Reader[Any, Output]
    type Footer = Option[Output]
    type Status = Vector[Output]

  trait Interpreter:
    def render(el: UIElement): Rendered
