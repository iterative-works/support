// PURPOSE: Tests for the Czech yes/no enum declaration helper — the extension resolves
// PURPOSE: through a plain wildcard import and produces the ano/ne enum segment

package works.iterative.forms.czech

import zio.test.*
import portaly.forms.*

object YesNoEnumSpec extends ZIOSpecDefault:
    def spec = suite("Enum.yesno")(
        test("declares the ano/ne enum through the extension") {
            assertTrue(
                Enum.yesno("souhlas") ==
                    Enum("souhlas", List("ano", "ne"), default = None, optional = true)
            )
        },
        test("required declaration keeps the optional flag") {
            assertTrue(!Enum.yesno("souhlas", optional = false).optional)
        },
        test("boolean default carries over as its string form") {
            assertTrue(Enum.yesno("souhlas", default = Some(true)).default == Some("true"))
        }
    )
end YesNoEnumSpec
