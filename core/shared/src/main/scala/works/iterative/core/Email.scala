package works.iterative.core

import zio.prelude.*

opaque type Email = String

object Email extends ValidatedStringFactory[Email](e => e):
    def apply(value: String): Validated[Email] =
        // The regex below is taken from the HTML5 spec for "email address state" (https://html.spec.whatwg.org/multipage/input.html#valid-e-mail-address)
        // and is the most permissive regex we can use that still conforms to the spec.
        // Copilot work.
        val regex = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9]" +
            "(?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?" +
            "(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"

        Validation.fromPredicateWith(UserMessage("error.invalid.email"))(value)(
            _.matches(regex)
        )
    end apply
end Email
