package works.iterative.ui.model.tables

/** A column in a table
  *
  * @param name
  *   the name of the column, must be unique in a row
  * @param get
  *   a function to get the value of the column from a type
  */
case class Column[A, Cell](name: String, get: A => Cell)

/** A typeclass to represet a type that can be tabulated into Cells */
trait Tabular[A, Cell]:
  def columns: List[Column[A, Cell]]
