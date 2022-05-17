package works.iterative.services.files
package components.tailwind

import com.raquo.laminar.api.L.{*, given}
import works.iterative.ui.components.tailwind.Icons
import works.iterative.ui.components.tailwind.HtmlRenderable

given HtmlRenderable[File] with
  def toHtml(m: File): HtmlElement =
    li(
      cls("pl-3 pr-4 py-3 flex items-center justify-between text-sm"),
      div(
        cls("w-0 flex-1 flex items-center"),
        Icons.solid.paperclip("w-5 h-5 flex-shrink-0 text-gray-400"),
        span(cls("ml-2 flex-1 w-0 truncate"), m.name)
      ),
      a(
        href(m.url),
        cls("font-medium text-indigo-600 hover:text-indigo-500"),
        "Otevřít"
      )
    )
