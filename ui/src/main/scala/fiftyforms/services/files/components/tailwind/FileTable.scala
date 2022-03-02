package fiftyforms.services.files.components.tailwind

import com.raquo.laminar.api.L.{*, given}
import com.raquo.domtypes.generic.codecs.StringAsIsCodec
import fiftyforms.ui.components.tailwind.Icons

def FileTable(
    files: Signal[List[File]],
    selectedFiles: Var[Set[File]]
): HtmlElement =
  val scope = customHtmlAttr("scope", StringAsIsCodec)

  def headerRow: HtmlElement =
    val col = scope("col")
    val baseM: Modifier[HtmlElement] = Seq(cls("px-6 py-3"), col)
    val textH = cls(
      "text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
    )
    tr(
      th(baseM, span(cls("sr-only"), "Vybrat")),
      th(baseM, textH, "Název"),
      th(baseM, cls("relative"), span(cls("sr-only"), "Otevřít"))
    )

  def tableRow(
      f: File,
      idx: Int,
      selected: Boolean
  )(toggleSelection: Observer[Unit]): HtmlElement =
    val baseC = cls("px-6 py-4 whitespace-nowrap text-sm")
    tr(
      cls(if idx % 2 == 0 then "bg-gray-50" else "bg-white"),
      td(
        cls("font-medium cursor-pointer"),
        onClick.mapTo(()) --> toggleSelection,
        cls(if selected then "text-green-900" else "text-gray-200"),
        Icons.outline.`check-circle`().amend(svg.cls := "mx-auto"),
        span(cls("sr-only"), if selected then "Vybráno" else "Nevybráno")
      ),
      td(
        baseC,
        cls("font-medium text-gray-900"),
        f.name,
        onClick.mapTo(()) --> toggleSelection
      ),
      td(
        baseC,
        cls("text-right font-medium"),
        a(
          href(f.url),
          target("_blank"),
          cls(
            "flex items-center space-x-4 text-indigo-600 hover:text-indigo-900"
          ),
          Icons.outline.`external-link`(),
          "Otevřít"
        )
      )
    )

  div(
    cls("flex flex-col"),
    div(cls("overflow-x-auto sm:-mx-6 lg:-mx-8")),
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
            children <-- files
              .map(_.zipWithIndex)
              .combineWithFn(selectedFiles)((f, sel) =>
                f.map((file, idx) =>
                  val active = sel.contains(file)
                  tableRow(file, idx, active)(
                    selectedFiles.writer
                      .contramap(_ => if active then sel - file else sel + file)
                  )
                )
              )
          )
        )
      )
    )
  )
