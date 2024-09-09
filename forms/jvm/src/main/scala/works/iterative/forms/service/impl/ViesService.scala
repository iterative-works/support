package portaly.forms.service.impl

import zio.*
import works.iterative.tapir.CustomTapir.*
import sttp.client3.*
import sttp.client3.ziojson.*
import zio.json.*
import Vies.*

class ViesService(backend: Backend, config: ViesConfig):
    def check(country: String, vatId: String): UIO[Option[Boolean]] =
        basicRequest
            .post(
                uri"https://ec.europa.eu/taxation_customs/vies/rest-api/check-vat-number"
            )
            .body(
                Vies.Request(
                    country,
                    vatId,
                    config.requesterCountryCode,
                    config.requesterNumber
                )
            )
            .header("Accept", "application/json")
            .response(asJson[Vies.Response])
            .responseGetRight
            .send(backend)
            .map(_.body.valid)
            .logError
            .option
end ViesService

object ViesService:
    import Config.*
    given config: Config[ViesConfig] =
        (string("requesterCountryCode").withDefault("CZ") ++
            string("requesterNumber") ++ setOf("availableCountries", string).withDefault(
                ViesConfig.defaultEuCountries
            )).nested("vies").map((r, n, a) => ViesConfig(r, n, a))

    val layer: URLayer[BackendProvider, ViesService] =
        import zio.config.typesafe.*
        ZLayer {
            for
                provider <- ConfigProvider.fromResourcePathZIO()
                conf <- provider.load(config)
                backend <- ZIO.service[BackendProvider]
            yield ViesService(backend.get, conf)
        }.orDie
    end layer

    def check(
        country: String,
        vatId: String
    ): URIO[ViesService, Option[Boolean]] =
        ZIO.serviceWithZIO(_.check(country, vatId))
end ViesService
