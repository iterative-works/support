package cz.e_bs.cmi.mdr.pdb.app.components.files

import com.raquo.laminar.api.L.{*, given}
import cz.e_bs.cmi.mdr.pdb.app.components.Icons

case class File(name: String, url: String)

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
        div(
          cls("ml-4 flex-shrink-0 flex space-x-4"),
          a(
            href(m.url),
            cls("font-medium text-indigo-600 hover:text-indigo-500"),
            "Otevřít"
          ),
          span(cls("text-gray-300"), "|"),
          a(
            href("#"),
            cls("font-medium text-indigo-600 hover:text-indigo-500"),
            "Odebrat"
          )
        )
      )

case class FileList(
    files: Signal[List[File]]
)

object FileList:
  extension (m: FileList)
    def toHtml: HtmlElement =
      div(
        ul(
          role("list"),
          cls("border border-gray-200 rounded-md divide-y divide-gray-200"),
          children <-- m.files.map(_.map(_.toHtml))
        ),
        button(
          tpe := "button",
          cls := "mt-5 bg-white py-2 px-3 border border-gray-300 rounded-md shadow-sm text-sm leading-4 font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500",
          "Přidat soubor"
        )
      )
