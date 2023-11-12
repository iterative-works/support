package works.iterative.core

import zio.prelude.*

opaque type Phone = String

object Phone extends ValidatedStringFactory[Phone](e => e):
  def apply(value: String): Validated[Phone] =
    // The regex to validate international phone numbers with optional country code
    val regex =
      "^(?:\\+?(\\d{1,3}))?[-. (]*(\\d{3})[-. )]*(\\d{3})[-. ]*(\\d{2,4})(?: *x(\\d+))?$"

    Validation.fromPredicateWith(UserMessage("error.invalid.phone"))(value)(
      _.matches(regex)
    )
