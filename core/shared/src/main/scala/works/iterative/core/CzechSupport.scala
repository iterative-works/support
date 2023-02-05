package works.iterative.core

trait CzechSupport:
  given czechOrdering: Ordering[String]

object CzechSupport extends CzechSupport with CzechSupportPlatformSpecific
