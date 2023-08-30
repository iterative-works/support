package works.iterative.ui.components.laminar

import com.raquo.laminar.api.L.*
import works.iterative.ui.model.Computable
import works.iterative.ui.model.Computable.*
import com.raquo.laminar.tags.HtmlTag
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.raquo.laminar.nodes.CommentNode
import org.scalajs.dom

class ComputableComponent[Ref <: dom.html.Element](
    as: HtmlTag[Ref],
    mods: Mod[ReactiveHtmlElement[Ref]]*
)(
    c: Signal[Computable[HtmlElement]]
):
  val element: ReactiveHtmlElement[Ref] = as(
    mods,
    child <-- c.map {
      case Uninitialized           => CommentNode("Uninitialized")
      case Computing(_)            => CommentNode("Computing")
      case Ready(element)          => element
      case Failed(_)               => CommentNode("Failed")
      case Recomputing(_, element) => element
    }
  )
