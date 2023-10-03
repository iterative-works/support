package works.iterative.ui.components.laminar

import com.raquo.laminar.api.L.*
import works.iterative.ui.model.Computable
import works.iterative.ui.model.Computable.*
import com.raquo.laminar.tags.HtmlTag
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.raquo.laminar.nodes.CommentNode
import org.scalajs.dom
import works.iterative.ui.components.ComponentContext
import works.iterative.core.UserMessage
import LaminarExtensions.*

class ComputableComponent[Ref <: dom.html.Element](
    as: HtmlTag[Ref],
    mods: Mod[ReactiveHtmlElement[Ref]]*
)(
    c: Signal[Computable[HtmlElement]]
)(using ComponentContext[?]):
  val element: ReactiveHtmlElement[Ref] = as(
    mods,
    child <-- c.map {
      case Uninitialized => CommentNode("Uninitialized")
      case Computing(_) =>
        div(
          cls("text-center"),
          h3(
            cls("mt-2 text-sm font-semibold text-gray-900"),
            UserMessage("loading").asElement
          ),
          UserMessage("loading.description").asOptionalElement.map(dm =>
            p(cls("mt-1 text-sm text-gray-500"), dm)
          )
        )
      case Ready(element)          => element
      case Failed(_)               => CommentNode("Failed")
      case Recomputing(_, element) => element
    }
  )
