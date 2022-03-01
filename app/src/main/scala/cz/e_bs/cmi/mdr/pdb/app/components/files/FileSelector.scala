package cz.e_bs.cmi.mdr.pdb.app.components.files

import com.raquo.laminar.api.L.{*, given}
import com.raquo.domtypes.generic.codecs.StringAsIsCodec
import cz.e_bs.cmi.mdr.pdb.app.components.Icons
import io.laminext.syntax.core.{*, given}
import com.raquo.domtypes.generic.codecs.BooleanAsTrueFalseStringCodec

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

def FileSelector(
    files: Signal[List[File]],
    selectedFiles: Var[Set[File]]
): HtmlElement =
  div(
    cls(
      "inline-block transform overflow-hidden rounded-lg bg-white text-left align-bottom shadow-xl transition-all sm:my-8 sm:w-full sm:max-w-lg sm:align-middle"
    ),
    role("dialog"),
    customHtmlAttr("aria.modal", BooleanAsTrueFalseStringCodec)(true),
    aria.labelledBy("modal-headline"),
    div(
      cls("bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4"),
      div(
        cls("sm:flex sm:items-start"),
        div(
          cls("mt-3 text-center sm:mt-0 sm:ml-4 sm:text-left"),
          h3(
            cls("text-lg font-medium leading-6 text-gray-900"),
            idAttr("modal-headline"),
            "Výběr souborů"
          )
        )
      ),
      FileTable(files, selectedFiles)
    ),
    div(
      cls("bg-gray-50 px-4 py-3 sm:flex sm:flex-row-reverse sm:px-6"),
      span(
        cls("flex w-full rounded-md shadow-sm sm:ml-3 sm:w-auto"),
        button(
          typ("button"),
          cls(
            "focus:shadow-outline-green inline-flex w-full justify-center rounded-md border border-transparent bg-indigo-600 px-4 py-2 text-base font-medium leading-6 text-white shadow-sm transition duration-150 ease-in-out hover:bg-indigo-500 focus:border-indigo-700 focus:outline-none sm:text-sm sm:leading-5"
          ),
          "Potvrdit"
        )
      ),
      span(
        cls("mt-3 flex w-full rounded-md shadow-sm sm:mt-0 sm:w-auto"),
        button(
          typ("button"),
          cls(
            "focus:shadow-outline-blue inline-flex w-full justify-center rounded-md border border-gray-300 bg-white px-4 py-2 text-base font-medium leading-6 text-gray-700 shadow-sm transition duration-150 ease-in-out hover:text-gray-500 focus:border-blue-300 focus:outline-none sm:text-sm sm:leading-5"
          ),
          "Zrušit"
        )
      )
    )
  )
