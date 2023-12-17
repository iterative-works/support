package works.iterative.service.payment
package impl.comgate

import zio.*
import zio.config.*
import sttp.client3.*
import sttp.client3.ziojson.*
import works.iterative.tapir.CustomTapir.*

case class ComGateConfig(
    merchant: String,
    secret: String,
    baseUrl: String = ComGateLive.DEFAULT_URL,
    test: Boolean = true
) {
  def matchCredentials(merchant: String, secret: String): Boolean =
    merchant == this.merchant && secret == this.secret
}

case class ComGateLive(backend: Backend, config: ComGateConfig)
    extends PayGate {
  def methods: UIO[MethodsResponse] = {
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
  }.orDie

  override def check: UIO[Unit] = methods.unit

  override def create(info: PaymentInfo): UIO[Created] = {
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
  }.orDie

  override def handleNotify(result: Seq[(String, String)]): UIO[Processed] = {
    PaymentResult
      .fromParams(result)(using config)
      .toZIO
      .mapBoth(
        msg => new IllegalArgumentException(msg),
        r =>
          Processed(r.transId, r.refId, r.price, r.status == PaymentStatus.Paid)
      )
  }.orDie
}

object ComGateLive:
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

  val live: URLayer[Backend & ComGateConfig, PayGate] =
    ZLayer {
      for
        c <- ZIO.service[ComGateConfig]
        b <- ZIO.service[Backend]
      yield ComGateLive(b, c)
    }

  /*
   * A live layer that makes sure the test parameter is set, without regard for config.
   */
  val test: URLayer[Backend & ComGateConfig, PayGate] =
    ZLayer {
      for
        c <- ZIO.service[ComGateConfig]
        b <- ZIO.service[Backend]
      yield ComGateLive(b, c.copy(test = true))
    }
