package works.iterative.ui.components

trait TableUIModule[T]:
    self: UIStylesModule[T] =>

    // Column definition
    case class Column[A](
        header: String,
        render: A => T,
        className: A => String = (_: A) => ""
    )

    // Enhanced table builder
    case class TableBuilder[A](
        columns: Seq[Column[A]],
        data: Seq[A],
        variant: String = "default",
        override val attributes: Map[String, String] = Map.empty,
        override val classes: Seq[String] = Seq.empty,
        override val dataset: Map[String, String] = Map.empty,
        override val styleOverride: UIStylesModule[T]#StyleOverride = createStyleOverride()
    ) extends StylableUIComponentBuilder[T, TableBuilder[A]]:
        override protected val stylesModule: UIStylesModule[T] = TableUIModule.this
        override protected def componentBasePath: String = "table"

        def withVariant(variant: String): TableBuilder[A] =
            copy(variant = variant)

        protected def withAttributes(newAttrs: Map[String, String]): TableBuilder[A] =
            copy(attributes = newAttrs)

        protected def withCls(newClasses: Seq[String]): TableBuilder[A] =
            copy(classes = newClasses)

        protected def withDataset(newDataset: Map[String, String]): TableBuilder[A] =
            copy(dataset = newDataset)

        protected def withStyleOverrides(newOverride: UIStylesModule[T]#StyleOverride)
            : TableBuilder[A] =
            copy(styleOverride = newOverride)

        // Style helper methods for table-specific parts
        def withTableStyle(styles: Map[String, String]): TableBuilder[A] =
            withStyleOverride("table", styles)

        def withHeaderStyle(styles: Map[String, String]): TableBuilder[A] =
            withPartStyle("header", styles)

        def withRowStyle(styles: Map[String, String]): TableBuilder[A] =
            withPartStyle("row", styles)

        def withCellStyle(styles: Map[String, String]): TableBuilder[A] =
            withPartStyle("cell", styles)

        // Class helper methods
        def withTableClasses(classes: Seq[String]): TableBuilder[A] =
            withClassOverride("table", classes)

        def withHeaderClasses(classes: Seq[String]): TableBuilder[A] =
            withPartClasses("header", classes)

        def withRowClasses(classes: Seq[String]): TableBuilder[A] =
            withPartClasses("row", classes)

        def withCellClasses(classes: Seq[String]): TableBuilder[A] =
            withPartClasses("cell", classes)

        // Get computed styles with overrides
        def tableStyle: Map[String, String] =
            styleOverride.getComputedStyle("table", variant)

        def headerStyle: Map[String, String] =
            styleOverride.getComputedStyle("table.header", variant)

        def headerRowStyle: Map[String, String] =
            styleOverride.getComputedStyle("table.header.row", variant)

        def headerCellStyle: Map[String, String] =
            styleOverride.getComputedStyle("table.header.cell", variant)

        def bodyStyle: Map[String, String] =
            styleOverride.getComputedStyle("table.body", variant)

        def rowStyle: Map[String, String] =
            styleOverride.getComputedStyle("table.row", variant)

        def cellStyle: Map[String, String] =
            styleOverride.getComputedStyle("table.cell", variant)

        // Get computed classes with overrides
        def tableClasses: Seq[String] =
            styleOverride.getComputedClasses("table", variant) ++ classes

        def headerClasses: Seq[String] =
            styleOverride.getComputedClasses("table.header", variant)

        def headerRowClasses: Seq[String] =
            styleOverride.getComputedClasses("table.header.row", variant)

        def headerCellClasses: Seq[String] =
            styleOverride.getComputedClasses("table.header.cell", variant)

        def bodyClasses: Seq[String] =
            styleOverride.getComputedClasses("table.body", variant)

        def rowClasses: Seq[String] =
            styleOverride.getComputedClasses("table.row", variant)

        def cellClasses: Seq[String] =
            styleOverride.getComputedClasses("table.cell", variant)

        def render: T = renderTable(this)
    end TableBuilder

    // Factory method
    def table[A](columns: Seq[Column[A]], data: Seq[A]): TableBuilder[A] =
        TableBuilder(columns, data)

    // Protected rendering method
    protected def renderTable[A](builder: TableBuilder[A]): T
end TableUIModule
