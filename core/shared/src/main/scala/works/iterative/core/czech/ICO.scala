package works.iterative.core
package czech

import zio.prelude.*

opaque type ICO = String

object ICO extends ValidatedStringFactory[ICO](e => e):
    // Expects "normalized" IC string (no spaces)
    def isValidCzechIC(icNumber: String): Boolean =
        val rest = icNumber.toList
            .zip(8 to 2 by -1)
            .map { case (digit, k) =>
                String.valueOf(digit).toInt * k
            }
            .sum % 11
        val last = String.valueOf(icNumber.last).toInt
        (11 - rest) % 10 == last
    end isValidCzechIC

    def apply(value: String): Validated[ICO] =
        for
            _ <- Validation.fromPredicateWith(UserMessage("error.invalid.ic.format"))(
                value
            )(
                _.matches("^\\d{8}$")
            )
            _ <- Validation.fromPredicateWith(UserMessage("error.invalid.ic"))(
                value
            )(isValidCzechIC(_))
        yield value
end ICO
