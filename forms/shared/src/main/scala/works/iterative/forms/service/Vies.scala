package portaly.forms.service.impl

import zio.json.*
import works.iterative.tapir.CustomTapir.*

object Vies:
    final case class Request(
        countryCode: String,
        vatNumber: String,
        requesterCountryCode: String,
        requesterVatNumber: String
    ) derives JsonCodec,
          Schema

    final case class Response(
        countryCode: String,
        vatNumber: String,
        requestDate: String,
        valid: Boolean,
        name: String,
        address: String
    ) derives JsonCodec,
          Schema
end Vies

case class ViesConfig(
    requesterCountryCode: String,
    requesterNumber: String,
    availableCountries: Set[String]
)

object ViesConfig:
    def apply(requesterCountryCode: String, requesterNumber: String): ViesConfig =
        ViesConfig(
            requesterCountryCode,
            requesterNumber,
            defaultEuCountries
        )

    def apply(requesterNumber: String): ViesConfig =
        ViesConfig(
            "CZ",
            requesterNumber,
            defaultEuCountries
        )

    val defaultEuCountries: Set[String] =
        // A set of EU member state country codes
        Set(
            "AT",
            "BE",
            "BG",
            "CY",
            "CZ",
            "DE",
            "DK",
            "EE",
            "EL",
            "ES",
            "FI",
            "FR",
            "HR",
            "HU",
            "IE",
            "IT",
            "LT",
            "LU",
            "LV",
            "MT",
            "NL",
            "PL",
            "PT",
            "RO",
            "SE",
            "SI",
            "SK"
        )
end ViesConfig
