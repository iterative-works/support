package works.iterative.ui.components.laminar.tables

import com.raquo.laminar.api.L.{*, given}
import works.iterative.ui.components.laminar.HtmlTabular
import works.iterative.ui.model.tables.Tabular
import works.iterative.core.UserMessage
import works.iterative.ui.components.tailwind.laminar.LaminarExtensions.given
import works.iterative.ui.components.tailwind.ComponentContext
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html

trait HtmlTableBuilderModule:

  trait TableUIFactory:
    def table(headerRows: ReactiveHtmlElement[html.TableRow]*)(
        bodyRows: ReactiveHtmlElement[html.TableRow]*
    ): ReactiveHtmlElement[html.Table]
    def headerRow(mod: HtmlMod)(
        headerCells: ReactiveHtmlElement[html.TableCell]*
    ): ReactiveHtmlElement[html.TableRow]
    def dataRow(mod: HtmlMod)(
        dataCells: ReactiveHtmlElement[html.TableCell]*
    ): ReactiveHtmlElement[html.TableRow]
    def headerCell(content: HtmlMod): ReactiveHtmlElement[html.TableCell]
    def dataCell(content: HtmlMod): ReactiveHtmlElement[html.TableCell]

  def tableHeaderResolver: TableHeaderResolver
  def tableUIFactory: TableUIFactory

  def buildTable[A: HtmlTabular](data: List[A]): HtmlTableBuilder[A] =
    HtmlTableBuilder[A](data)

  case class HtmlTableBuilder[A: HtmlTabular](
      data: List[A],
      headerRowMod: HtmlMod = emptyMod,
      dataRowMod: (A, Int) => HtmlMod = (_: A, _) => emptyMod,
      headerCellMod: String => HtmlMod = _ => emptyMod,
      dataCellMod: (String, A) => HtmlMod = (_, _: A) => emptyMod
  ):

    def headerRowMod(mod: HtmlMod): HtmlTableBuilder[A] =
      copy(headerRowMod = mod)

    def dataRowMod(mod: A => HtmlMod): HtmlTableBuilder[A] =
      copy(dataRowMod = (a, _) => mod(a))

    def dataRowMod(mod: (A, Int) => HtmlMod): HtmlTableBuilder[A] =
      copy(dataRowMod = mod)

    def headerCellMod(mod: String => HtmlMod): HtmlTableBuilder[A] =
      copy(headerCellMod = mod)

    def dataCellMod(mod: (String, A) => HtmlMod): HtmlTableBuilder[A] =
      copy(dataCellMod = mod)

    def build: HtmlElement =
      val tab = summon[HtmlTabular[A]]
      tableUIFactory.table(
        tableUIFactory.headerRow(headerRowMod)(
          tab.columns.map(_.name).map { n =>
            tableUIFactory.headerCell(
              Seq[HtmlMod](headerCellMod(n), tableHeaderResolver(n))
            )
          }*
        )
      )(
        data.zipWithIndex.map((d, idx) =>
          tableUIFactory.dataRow(dataRowMod(d, idx))(
            tab.columns
              .map(c => c.name -> c.get(d))
              .map { (n, v) =>
                tableUIFactory.dataCell(Seq(v, dataCellMod(n, d)))
              }*
          )
        )*
      )
