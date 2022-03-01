package cz.e_bs.cmi.mdr.pdb.app.components.files

import com.raquo.laminar.api.L.{*, given}
import com.raquo.domtypes.generic.codecs.StringAsIsCodec

def FileTable(files: Signal[List[File]]): HtmlElement =
  val scope = customHtmlAttr("scope", StringAsIsCodec)

  def headerRow: HtmlElement =
    val col = scope("col")
    val baseM: Modifier[HtmlElement] = Seq(cls("px-6 py-3"), col)
    val textH = cls(
      "text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
    )
    tr(
      th(baseM, textH, "Název"),
      th(baseM, textH, "Kategorie"),
      th(baseM, cls("relative"), span(cls("sr-only"), "Otevřít"))
    )

  def tableRow(f: File, idx: Int): HtmlElement =
    val baseC = cls("px-6 py-4 whitespace-nowrap text-sm")
    tr(
      cls(if idx % 2 == 0 then "bg-gray-50" else "bg-white"),
      td(baseC, cls("font-medium text-gray-900"), f.name),
      td(baseC, cls("text-gray-500"), "kategorie"),
      td(baseC, cls("text-right font-medium"), a(href(f.url), "Otevřít"))
    )

  div(
    cls("flex flex-col"),
    div(cls("-my-2 overflow-x-auto sm:-mx-6 lg:-mx-8")),
    div(
      cls("py-2 align-middle inline-block min-w-full sm:px-6 lg:px-8"),
      div(
        cls("shadow overflow-hidden border-b border-gray-200 sm:rounded-lg"),
        table(
          cls("min-w-full divide-y divide-gray-200"),
          thead(
            cls("bg-gray-50"),
            headerRow
          ),
          tbody(
            children <-- files.map(_.zipWithIndex.map(tableRow))
          )
        )
      )
    )
  )

def FileSelector(files: Signal[List[File]]): HtmlElement =
  div(FileTable(files))
