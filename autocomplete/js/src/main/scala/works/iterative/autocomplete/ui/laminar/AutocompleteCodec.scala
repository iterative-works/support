package works.iterative.autocomplete.ui.laminar

import works.iterative.autocomplete.AutocompleteEntry
import works.iterative.core.{Validated, ValidatedStringFactory}

trait AutocompleteCodec[A]:
    /** Return either whole entry, or entry value */
    def encode(a: A): Either[String, AutocompleteEntry]
    def decode(e: AutocompleteEntry): Validated[A]
end AutocompleteCodec

object AutocompleteCodec:
    def fromValidatedStringFactory[A](using factory: ValidatedStringFactory[A]) =
        new AutocompleteCodec[A]:
            override def encode(a: A): Either[String, AutocompleteEntry] = Left(factory.getter(a))
            override def decode(e: AutocompleteEntry): Validated[A] = factory(e.value)
end AutocompleteCodec
