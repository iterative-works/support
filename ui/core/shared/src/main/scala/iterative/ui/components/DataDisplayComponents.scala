package works.iterative.ui.components

trait DataDisplayComponents[T] extends Components[T]:
    case class Column[A](
        header: String,
        render: A => T,
        className: String = ""
    )
    def table[A](
        columns: Seq[Column[A]],
        data: Seq[A],
        containerClassName: String = "",
        tableClassName: String = "",
        headerClassName: String = "",
        rowClassName: String = ""
    ): T

    def link(text: String, href: String): T

    def actionButton(
        text: String,
        attributes: Map[String, String],
        variant: String = "primary"
    ): T
end DataDisplayComponents
