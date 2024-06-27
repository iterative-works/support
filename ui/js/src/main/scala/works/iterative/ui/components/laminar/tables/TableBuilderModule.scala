package works.iterative.ui.components.laminar.tables

import com.raquo.laminar.api.L.*
import works.iterative.ui.components.laminar.HtmlTabular
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html

trait TableUIFactory:
    def container(table: ReactiveHtmlElement[html.Table]): HtmlElement
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
end TableUIFactory

trait HtmlTableBuilderModule:
    def tableHeaderResolver: TableHeaderResolver
    def tableUIFactory: TableUIFactory

    def buildTable[A: HtmlTabular](data: List[A]): HtmlTableBuilder[A] =
        HtmlTableBuilder[A](data)

    case class HtmlTableBuilder[A](
        data: List[A],
        headerRowMod: HtmlMod = emptyMod,
        dataRowMod: (A, Int) => HtmlMod = (_: A, _) => emptyMod,
        headerCellMod: String => HtmlMod = _ => emptyMod,
        dataCellMod: (String, A) => HtmlMod = (_, _: A) => emptyMod
    )(using tab: HtmlTabular[A]):
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

        def buildTableHeader: ReactiveHtmlElement[html.TableRow] =
            tableUIFactory.headerRow(headerRowMod)(
                tab.columns.map(_.name).map { n =>
                    tableUIFactory.headerCell(
                        Seq[HtmlMod](headerCellMod(n), tableHeaderResolver(n))
                    )
                }*
            )

        def buildTableData(
            data: List[A]
        ): List[ReactiveHtmlElement[html.TableRow]] =
            data.zipWithIndex.map((d, idx) =>
                tableUIFactory.dataRow(dataRowMod(d, idx))(
                    tab.columns
                        .map(c => c.name -> c.get(d))
                        .map { (n, v) =>
                            tableUIFactory.dataCell(Seq(v, dataCellMod(n, d)))
                        }*
                )
            )

        def build: HtmlElement = build()

        def build(tableMod: HtmlMod*): HtmlElement =
            tableUIFactory.container(
                tableUIFactory.table(buildTableHeader)(buildTableData(data)*).amend(tableMod)
            )
    end HtmlTableBuilder
end HtmlTableBuilderModule
