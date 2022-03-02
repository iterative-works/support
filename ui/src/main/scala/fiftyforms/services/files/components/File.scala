package fiftyforms.services.files.components

import com.raquo.laminar.api.L.{*, given}
import fiftyforms.ui.components.Icons

case class File(url: String, name: String)

object File:
  extension (m: File)
    def toHtml: HtmlElement =
      li(
        cls("pl-3 pr-4 py-3 flex items-center justify-between text-sm"),
        div(
          cls("w-0 flex-1 flex items-center"),
          Icons.solid
            .paperclip()
            .amend(svg.cls := "flex-shrink-0 text-gray-400"),
          span(cls("ml-2 flex-1 w-0 truncate"), m.name)
        ),
        a(
          href(m.url),
          cls("font-medium text-indigo-600 hover:text-indigo-500"),
          "Otevřít"
        )
      )
