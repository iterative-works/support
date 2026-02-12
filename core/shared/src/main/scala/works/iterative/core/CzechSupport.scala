package works.iterative.core

trait CzechSupport:
    given czechOrdering: Ordering[String]

// TODO: Fix cross-platform compilation in Mill
// object CzechSupport extends CzechSupport with CzechSupportPlatformSpecific
object CzechSupport extends CzechSupport:
    // Temporary implementation - should come from platform-specific
    given czechOrdering: Ordering[String] = Ordering.String
