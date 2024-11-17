package works.iterative.core

import ValidatedSyntax.*

/** Common methods for opaque validated String types
  */
trait ValidatedStringFactory[A](val getter: A => String):
    def apply(s: String): Validated[A]
    def unsafe(s: String): A = apply(s).orThrow
    extension (a: A) def value: String = getter(a)

    given ValidatedStringFactory[A] = this
end ValidatedStringFactory
