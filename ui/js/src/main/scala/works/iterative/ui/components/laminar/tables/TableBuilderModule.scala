package works.iterative.ui.components.laminar.tables

import com.raquo.laminar.api.L.{*, given}
import works.iterative.ui.components.laminar.HtmlTabular
import works.iterative.ui.model.tables.Tabular
import works.iterative.ui.components.laminar.tailwind.ui.TableComponentsModule
import works.iterative.core.UserMessage
import works.iterative.ui.components.tailwind.laminar.LaminarExtensions.given
import works.iterative.ui.components.tailwind.ComponentContext

trait HtmlTableBuilderModule:
  def buildTable[A: HtmlTabular](data: List[A]): HtmlTableBuilder[A]

  trait HtmlTableBuilder[A]:
    def headerRowMod(mod: HtmlMod): HtmlTableBuilder[A]
    def dataRowMod(mod: (A, Int) => HtmlMod): HtmlTableBuilder[A]
    def dataRowMod(mod: A => HtmlMod): HtmlTableBuilder[A] =
      dataRowMod((a, _) => mod(a))

    def headerCellMod(mod: String => HtmlMod): HtmlTableBuilder[A]
    def headerCellMod(mod: HtmlMod): HtmlTableBuilder[A] =
      headerCellMod(_ => mod)

    def dataCellMod(mod: (String, A) => HtmlMod): HtmlTableBuilder[A]
    def dataCellMod(mod: HtmlMod): HtmlTableBuilder[A] =
      dataCellMod((_, _) => mod)
    def dataCellMod(mod: String => HtmlMod): HtmlTableBuilder[A] =
      dataCellMod((s, _) => mod(s))

    def build: HtmlElement

trait HtmlTableBuilderModuleImpl(using resolver: TableHeaderResolver)
    extends HtmlTableBuilderModule:
  self: TableComponentsModule =>

  def buildTable[A: HtmlTabular](data: List[A]): HtmlTableBuilder[A] =
    new HtmlTableBuilderImpl[A](data)

  case class HtmlTableBuilderImpl[A: HtmlTabular](
      data: List[A],
      headerRowMod: HtmlMod = emptyMod,
      dataRowMod: (A, Int) => HtmlMod = (_: A, _) => emptyMod,
      headerCellMod: String => HtmlMod = _ => emptyMod,
      dataCellMod: (String, A) => HtmlMod = (_, _: A) => emptyMod
  ) extends HtmlTableBuilder[A]:

    def headerRowMod(mod: HtmlMod): HtmlTableBuilder[A] =
      copy(headerRowMod = mod)

    def dataRowMod(mod: (A, Int) => HtmlMod): HtmlTableBuilder[A] =
      copy(dataRowMod = mod)

    def headerCellMod(mod: String => HtmlMod): HtmlTableBuilder[A] =
      copy(headerCellMod = mod)

    def dataCellMod(mod: (String, A) => HtmlMod): HtmlTableBuilder[A] =
      copy(dataCellMod = mod)

    def build: HtmlElement =
      val tab = summon[HtmlTabular[A]]
      tables.simpleTable(
        tables.headerRow(headerRowMod)(
          tab.columns.map(_.name).map { n =>
            tables
              .headerCell(
                Seq[HtmlMod](headerCellMod(n), resolver(n))
              )
          }*
        )
      )(
        data.zipWithIndex.map((d, idx) =>
          tables.dataRow(dataRowMod(d, idx))(
            tab.columns
              .map(c => c.name -> c.get(d))
              .map { (n, v) =>
                tables.dataCell(Seq(v, dataCellMod(n, d)))
              }*
          )
        )
      )
