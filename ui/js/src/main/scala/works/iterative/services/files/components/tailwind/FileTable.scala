package works.iterative.services.files
package components.tailwind

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.codecs.StringAsIsCodec
import works.iterative.ui.components.tailwind.Icons
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.ZoneId
import java.util.Locale
import works.iterative.ui.TimeUtils
import works.iterative.core.CzechSupport

def FileTable(
    files: Signal[List[File]],
    maybeSelection: Option[Var[Set[File]]] = None
): HtmlElement =
  val scope = htmlAttr("scope", StringAsIsCodec)
  val selectedFiles = maybeSelection.getOrElse(Var(Set.empty))
  val openCategories = Var[Set[String]](
    maybeSelection.map(_.now().map(_.category)).getOrElse(Set.empty)
  )

  def headerRow: HtmlElement =
    val col = scope("col")
    val baseM: Modifier[HtmlElement] = Seq(cls("px-6 py-3"), col)
    val textH = cls(
      "text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
    )
    tr(
      maybeSelection.map(_ => th(baseM, span(cls("sr-only"), "Vybrat"))),
      th(baseM, textH, "Soubor"),
      th(baseM, cls("w-40"), textH, "Vytvořen"),
      th(baseM, cls("w-32 relative"), span(cls("sr-only"), "Otevřít"))
    )

  def tableRow(
      selected: File => Boolean,
      toggleSelection: File => Observer[Unit]
  )(
      f: File
  ): HtmlElement =
    val baseC = cls("px-6 py-4 whitespace-nowrap text-sm")
    tr(
      maybeSelection.map(_ =>
        td(
          cls("font-medium cursor-pointer"),
          onClick.mapTo(()) --> toggleSelection(f),
          cls(if selected(f) then "text-green-900" else "text-gray-200"),
          Icons.outline.`check-circle`("w-6 h-6 mx-auto"),
          span(cls("sr-only"), if selected(f) then "Vybráno" else "Nevybráno")
        )
      ),
      td(
        baseC,
        cls("font-medium text-gray-900"),
        f.name,
        onClick.mapTo(()) --> toggleSelection(f)
      ),
      td(
        baseC,
        cls("font-medium text-gray-600 text-right"),
        TimeUtils.formatDateTime(f.created),
        onClick.mapTo(()) --> toggleSelection(f)
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
          Icons.outline.`external-link`("w-6 h-6"),
          "Otevřít"
        )
      )
    )

  def category(
      renderRow: File => HtmlElement
  )(name: String, files: List[File]): Signal[List[HtmlElement]] =
    openCategories.signal.map(o =>
      tr(
        cls("border-t border-gray-200"),
        th(
          cls("cursor-pointer"),
          onClick.mapTo(
            if o.contains(name) then o - name else o + name
          ) --> openCategories,
          colSpan(if (maybeSelection.isDefined) then 4 else 3),
          scope("colgroup"),
          cls(
            "bg-gray-50 px-4 py-2 text-left text-sm font-semibold text-gray-900 sm:px-6"
          ),
          name
        )
      ) :: (if o.contains(name) then
              files.sortBy(_.name)(CzechSupport.czechOrdering).map(renderRow)
            else Nil)
    )

  div(
    cls("flex flex-col"),
    div(cls("overflow-x-auto sm:-mx-6 lg:-mx-8")),
    div(
      cls("py-2 align-middle inline-block min-w-full"),
      div(
        cls("shadow overflow-hidden border-b border-gray-200 sm:rounded-lg"),
        table(
          cls("min-w-full divide-y divide-gray-200"),
          thead(
            cls("bg-gray-50"),
            headerRow
          ),
          tbody(
            cls("bg-white"),
            children <-- files
              .combineWithFn(selectedFiles)((f, sel) =>
                val active = sel.contains
                val renderCategory = category(
                  tableRow(
                    active,
                    file =>
                      selectedFiles.writer.contramap(_ =>
                        if active(file) then sel - file else sel + file
                      )
                  )
                )
                Signal
                  .combineSeq(
                    f.groupBy(_.category)
                      .to(List)
                      .sortBy(_._1)(CzechSupport.czechOrdering)
                      .map(renderCategory(_, _))
                  )
                  .map(_.flatten)
              )
              .flatten
          )
        )
      )
    )
  )
