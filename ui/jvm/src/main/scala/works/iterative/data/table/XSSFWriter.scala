package works.iterative.data.table

import zio.*
import org.apache.poi.xssf.usermodel.XSSFRow
import org.apache.poi.ss.usermodel.CellStyle
import java.time.LocalDate
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.xssf.usermodel.XSSFCell
import org.apache.poi.common.usermodel.HyperlinkType

case class ExternalLink(
    name: String,
    href: String
)

trait CellWriter[T]:
    def write(cell: XSSFCell, value: T): Unit

object CellWriter:
    given CellWriter[String] with
        def write(cell: XSSFCell, value: String) = cell.setCellValue(value)

    given CellWriter[Int] with
        def write(cell: XSSFCell, value: Int) = cell.setCellValue(value)

    given CellWriter[LocalDate] with
        def write(cell: XSSFCell, value: LocalDate) = cell.setCellValue(value)

    given CellWriter[ExternalLink] with
        def write(cell: XSSFCell, value: ExternalLink) =
            cell.setCellValue(value.name)
            cell.setHyperlink(
                cell.getSheet.getWorkbook.getCreationHelper.createHyperlink(
                    HyperlinkType.URL
                )
            )
            cell.getHyperlink.setAddress(value.href)
        end write
    end given

    given optionalCellSetter[T](using CellWriter[T]): CellWriter[Option[T]] with
        def write(cell: XSSFCell, value: Option[T]) =
            value.foreach(summon[CellWriter[T]].write(cell, _))
end CellWriter

case class ColumnSpec[T, U: CellWriter](
    id: String,
    name: String,
    get: T => U,
    width: Int = 20
):
    def set(cell: XSSFCell, row: T) =
        summon[CellWriter[U]].write(cell, get(row))
end ColumnSpec

trait TableRowWriter[T]:
    def writeHeaderCells(row: XSSFRow)(using TableCellStyles): UIO[Unit]
    def writeDataCells(row: XSSFRow, value: T)(using TableCellStyles): UIO[Unit]
    def columnSizes: List[Int]
end TableRowWriter

class ColumnSpecsTableRowWriter[T](specs: List[ColumnSpec[T, ?]])
    extends TableRowWriter[T]:
    override def writeHeaderCells(row: XSSFRow)(using styles: TableCellStyles) =
        val headers = specs.map(_.name)
        ZIO.succeed {
            headers.zipWithIndex.foreach { case (header, index) =>
                val cell = row.createCell(index)
                cell.setCellValue(header)
                styles
                    .byName(TableCellStyles.HeaderStyleName)
                    .foreach(cell.setCellStyle)
            }
        }
    end writeHeaderCells

    override def writeDataCells(
        row: XSSFRow,
        value: T
    )(using styles: TableCellStyles): UIO[Unit] =
        ZIO.succeed {
            specs.zipWithIndex.map { case (spec, i) =>
                val cell = row.createCell(i)
                spec.set(cell, value)
                styles.byName(spec.id).foreach(cell.setCellStyle)
            }
        }.unit

    override def columnSizes: List[Int] =
        specs.map(_.width).toList
end ColumnSpecsTableRowWriter

type TableCellStyleDef = XSSFWorkbook => XSSFCellStyle

trait TableCellStyles:
    def byName(name: String): Option[CellStyle]

object TableCellStyles:
    val HeaderStyleName = "_header"

    val defaultHeaderStyle: TableCellStyleDef = (wb: XSSFWorkbook) =>
        val headerStyle = wb.createCellStyle()
        val font = wb.createFont()
        font.setBold(true)
        headerStyle.setFont(font)

        // set background color
        headerStyle.setFillForegroundColor(
            IndexedColors.GREY_25_PERCENT.getIndex()
        )
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND)

        // set border style
        headerStyle.setBorderTop(BorderStyle.MEDIUM)
        headerStyle.setBorderBottom(BorderStyle.MEDIUM)
        headerStyle.setBorderLeft(BorderStyle.MEDIUM)
        headerStyle.setBorderRight(BorderStyle.MEDIUM)
        headerStyle

    val dateStyle: TableCellStyleDef = (wb: XSSFWorkbook) =>
        val dateCellStyle = wb.createCellStyle()
        dateCellStyle.setDataFormat(
            wb
                .getCreationHelper()
                .createDataFormat()
                .getFormat("dd.MM.yyyy")
        )
        dateCellStyle

    val wrapStyle: TableCellStyleDef = (wb: XSSFWorkbook) =>
        // set cell style to shrink font size and truncate text
        val wrapCellStyle = wb.createCellStyle()
        wrapCellStyle.setWrapText(true)
        wrapCellStyle

    def make(): UIO[TableCellStyles] =
        ZIO.succeed(
            new TableCellStyles:
                override def byName(name: String): Option[CellStyle] =
                    None
        )

    def make(
        wb: XSSFWorkbook,
        styles: List[(String, TableCellStyleDef)]
    ): UIO[TableCellStyles] =
        ZIO
            .collectAll(styles.map { case (name, styleDef) =>
                for style <- ZIO.succeed(styleDef(wb))
                yield name -> style
            })
            .map { styles =>
                new TableCellStyles:
                    override def byName(name: String): Option[CellStyle] =
                        styles.toMap.get(name)
            }
end TableCellStyles

class TableWorkbook private (workbook: XSSFWorkbook, styles: TableCellStyles):
    def writeSheet[T](name: String, data: Iterable[T])(using
        writer: TableRowWriter[T]
    ): UIO[Unit] =
        for
            sheet <- ZIO.succeed(workbook.createSheet(name))
            headerRow <- ZIO.succeed(sheet.createRow(0))
            _ <- writer.writeHeaderCells(headerRow)(using styles)
            _ <- ZIO.foreach(data.zipWithIndex) { case (value, index) =>
                val row = sheet.createRow(index + 1)
                writer.writeDataCells(row, value)(using styles)
            }
            _ <- ZIO.foreach(writer.columnSizes.zipWithIndex) { case (size, index) =>
                ZIO.succeed(sheet.setColumnWidth(index, size * 256))
            }
        yield ()

    def toArray: UIO[Array[Byte]] = ZIO.succeed {
        val out = new java.io.ByteArrayOutputStream()
        workbook.write(out)
        out.close()
        out.toByteArray
    }
end TableWorkbook

object TableWorkbook:
    def make(): UIO[TableWorkbook] =
        for
            wb <- ZIO.succeed(XSSFWorkbook())
            styles <- TableCellStyles.make()
        yield TableWorkbook(wb, styles)

    def make(styles: List[(String, TableCellStyleDef)]): UIO[TableWorkbook] =
        for
            wb <- ZIO.succeed(XSSFWorkbook())
            styles <- TableCellStyles.make(wb, styles)
        yield TableWorkbook(wb, styles)
end TableWorkbook
