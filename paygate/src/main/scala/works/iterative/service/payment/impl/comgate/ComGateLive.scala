package works.iterative.service.payment
package impl.comgate

import zio.*
import zio.config.*
import sttp.client3.*
import sttp.client3.ziojson.*

case class ComGateConfig(
    merchant: String,
    secret: String,
    baseUrl: String = ComGateLive.DEFAULT_URL,
    test: Boolean = true
) {
  def matchCredentials(merchant: String, secret: String): Boolean =
    merchant == this.merchant && secret == this.secret
}

case class ComGateLive(
    backend: SttpBackend[Task, Any]
)(implicit config: ComGateConfig)
    extends PayGate {
  def methods: Task[MethodsResponse] = {
    basicRequest
      .post(uri"${config.baseUrl}/methods")
      .body(
        Map(
          "merchant" -> config.merchant,
          "secret" -> config.secret,
          "type" -> "json"
        )
      )
      .response(asJson[ComGateResponse[MethodsResponse]].map {
        case Left(err)                         => Left(err)
        case Right(ComGateResponse(Left(err))) => Left(err.error)
        case Right(ComGateResponse(Right(r)))  => Right(r)
      }.getRight)
      .send(backend)
      .map(_.body)
  }

  override def check: Task[Unit] = methods.unit

  override def create(info: PaymentInfo): Task[Created] = {
    basicRequest
      .post(uri"${config.baseUrl}/create")
      .body(
        Map(
          "merchant" -> config.merchant,
          "secret" -> config.secret,
          "test" -> config.test.toString,
          "country" -> "CZ",
          "price" -> (info.price * 100).toInt.toString,
          "curr" -> "CZK",
          "refId" -> info.vs,
          "email" -> info.email,
          "label" -> info.name,
          "method" -> "ALL",
          "lang" -> "cs",
          "prepareOnly" -> "true"
        )
      )
      .response(
        asParams
          .map {
            // Error page, leave as is
            case Left(msg) => Left(msg)
            case Right(params) =>
              CreateResponse.fromParams(params).toEitherWith(_.mkString("\n"))
          }
          .getRight
          .map(_.toCreated)
      )
      .send(backend)
      .map(_.body)
  }

  override def handleNotify(result: Seq[(String, String)]): Task[Processed] = {
    PaymentResult
      .fromParams(result)
      .toZIO
      .mapBoth(
        msg => new IllegalArgumentException(msg),
        r =>
          Processed(r.transId, r.refId, r.price, r.status == PaymentStatus.Paid)
      )
  }
}

object ComGateLive {
  type SttpClient = SttpBackend[Task, Any]

  val DEFAULT_URL: String = "https://payments.comgate.cz/v1.0"

  val configDescriptor: ConfigDescriptor[ComGateConfig] = {
    import ConfigDescriptor._
    (string("COMGATE_MERCHANT") zip string("COMGATE_SECRET") zip string(
      "COMGATE_URL"
    ).default(DEFAULT_URL) zip boolean("COMGATE_TEST")
      .default(true)).to[ComGateConfig]
  }

  val envConfig: ZLayer[Any, ReadError[String], ComGateConfig] =
    ZConfig.fromSystemEnv(configDescriptor)

  val live: URLayer[SttpClient with ComGateConfig, PayGate] =
    ZLayer.fromFunction((b: SttpClient, c: ComGateConfig) => ComGateLive(b)(c))

  /*
   * A live layer that makes sure the test parameter is set, without regard for config.
   */
  val test: URLayer[SttpClient with ComGateConfig, PayGate] =
    ZLayer.fromFunction((b: SttpClient, c: ComGateConfig) =>
      ComGateLive(b)(c.copy(test = true))
    )
}
