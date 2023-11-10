package works.iterative.ui.laminar

import com.raquo.laminar.api.L.*

trait InlineDisplay[T]:
  extension (t: T) def renderInline: Node

object InlineDisplay:
  given inlineString: InlineDisplay[String] with
    extension (t: String) def renderInline: Node = t
