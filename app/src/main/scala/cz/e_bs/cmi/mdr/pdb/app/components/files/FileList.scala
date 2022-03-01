package cz.e_bs.cmi.mdr.pdb.app.components.files

import com.raquo.laminar.api.L.{*, given}
import cz.e_bs.cmi.mdr.pdb.app.components.Icons

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
