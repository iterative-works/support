package works.iterative.ui.components.tailwind

import scala.quoted.*

/** Tailwind uses JIT compiler that needs to find relevant classes in the code in a form that is
  * recognizable - eg. "w-10", not ("w-" + "10")
  *
  * Macros compute the strings during compile time.
  */
object Macros:

    inline def size(edgeSize: Int): String = ${ sizeImpl('edgeSize) }

    private def sizeImpl(edgeSize: Expr[Int])(using Quotes): Expr[String] = '{
        "h-" + $edgeSize + " w-" + $edgeSize
    }
end Macros
