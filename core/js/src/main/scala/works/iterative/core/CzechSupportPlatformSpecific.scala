package works.iterative.core

import scala.scalajs.js

trait CzechSupportPlatformSpecific:
    given czechOrdering: Ordering[String] with
        def compare(x: String, y: String): Int =
            x.asInstanceOf[js.Dynamic].localeCompare(y, "cs-CZ").asInstanceOf[Int]
end CzechSupportPlatformSpecific
