package works.iterative.forms

trait FormContext:
    def getString(id: String): Option[String]
    def getInt(id: String): Option[Int] = getString(id).flatMap(_.toIntOption)

object FormContext:
    def empty: FormContext = new FormContext:
        def getString(name: String): Option[String] = None
