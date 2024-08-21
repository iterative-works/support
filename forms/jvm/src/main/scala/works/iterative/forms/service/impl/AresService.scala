package portaly.forms.service.impl

import zio.*
import works.iterative.tapir.CustomTapir.*
import sttp.client3.*
import sttp.client3.ziojson.*
import zio.json.*
import works.iterative.core.czech.ICO
import Ares.*

class AresService(backend: Backend):
    def subjekt(ico: ICO): UIO[Option[EkonomickySubjekt]] =
        basicRequest
            .get(
                uri"https://ares.gov.cz/ekonomicke-subjekty-v-be/rest/ekonomicke-subjekty/${ico.value}"
            )
            .header("Accept", "application/json")
            .response(asJson[EkonomickySubjekt])
            .responseGetRight
            .send(backend)
            .map(_.body)
            .logError
            .option
end AresService

object AresService:
    val layer: URLayer[BackendProvider, AresService] =
        ZLayer {
            for
                backend <- ZIO.service[BackendProvider]
            yield AresService(backend.get)
        }

    def subjekt(ico: ICO): URIO[AresService, Option[EkonomickySubjekt]] =
        ZIO.serviceWithZIO(_.subjekt(ico))
end AresService
