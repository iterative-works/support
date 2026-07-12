// PURPOSE: Tests for the VIES VAT-check configuration — requester defaults and the
// PURPOSE: EU member-state country set the validation is limited to

package works.iterative.forms.czech

import zio.test.*
import portaly.forms.service.impl.ViesConfig

object ViesConfigSpec extends ZIOSpecDefault:
    def spec = suite("ViesConfig")(
        test("single-argument construction defaults the requester country to CZ") {
            val config = ViesConfig("00177016")
            assertTrue(
                config.requesterCountryCode == "CZ",
                config.requesterNumber == "00177016",
                config.availableCountries == ViesConfig.defaultEuCountries
            )
        },
        test("the EU country set uses VIES codes") {
            assertTrue(
                ViesConfig.defaultEuCountries.size == 27,
                ViesConfig.defaultEuCountries.contains("EL"),
                !ViesConfig.defaultEuCountries.contains("GB")
            )
        }
    )
end ViesConfigSpec
