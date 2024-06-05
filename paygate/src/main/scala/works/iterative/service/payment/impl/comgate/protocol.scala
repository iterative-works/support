package works.iterative.service.payment
package impl.comgate

import zio.json.JsonDecoder
import zio.json.DeriveJsonDecoder
import zio.prelude.Validation
import scala.util.Try

case class ComGateCredentials(merchant: String, secret: String)

case class ComGateError(
    code: Int,
    message: String,
    extraMessage: Option[String]
) extends RuntimeException(
        s"ComGate error response [$code]: $message${extraMessage.map("\n" + _).getOrElse("")}"
    )

object ComGateError:
    implicit val decoder: JsonDecoder[ComGateError] =
        DeriveJsonDecoder.gen[ComGateError]

case class ErrorResponse(error: ComGateError)

object ErrorResponse:
    implicit val decoder: JsonDecoder[ErrorResponse] =
        DeriveJsonDecoder.gen[ErrorResponse]

sealed trait SuccessResponse

case class Method(id: String, name: String, description: String, logo: String)

object Method:
    implicit val decoder: JsonDecoder[Method] = DeriveJsonDecoder.gen[Method]

case class MethodsResponse(methods: List[Method]) extends SuccessResponse

object MethodsResponse:
    implicit val decoder: JsonDecoder[MethodsResponse] =
        DeriveJsonDecoder.gen[MethodsResponse]

/*
 * Response from API create method, if valid, contains transId and redirect.
 * According to the documentation, the response has code and message always present,
 * which are possible error values, and optional transId and redirect only when successful.
 * We do not need the code and message, only for errors, which get validated away in CreateResponse#fromParams.
 */
case class CreateResponse(
    transId: String,
    redirect: String
):
    // We have ComGate-specific CreateResponse, and generic Created
    def toCreated: Created = Created(transId, redirect)
end CreateResponse

case class ParamMap(params: Seq[(String, String)]):
    private val m = params.toMap

    def get(key: String): Option[String] = m.get(key).map(_.trim)

    def optional(key: String): Validation[Nothing, Option[String]] =
        Validation.succeed(get(key))

    def required(key: String): Validation[String, String] =
        Validation.fromOptionWith(s"chybí parametr '$key'")(get(key))

    def nonEmpty(key: String): Validation[String, String] =
        for
            v <- required(key)
            result <- Validation.fromPredicateWith(s"parametr '$key' je prázdný")(v)(
                _.nonEmpty
            )
        yield result

    def boolean(key: String): Validation[String, Boolean] =
        for
            v <- nonEmpty(key)
            result <-
                if v == "true" then Validation.succeed(true) else Validation.succeed(false)
        yield result

    def int(key: String): Validation[String, Int] =
        for
            v <- nonEmpty(key)
            result <- Validation
                .fromTry(Try(v.toInt))
                .asError(
                    s"Nesprávný formát parametru $key, očekáván integer, hodnota je '$v'"
                )
        yield result
end ParamMap

object CreateResponse:
    def fromParams(
        params: Seq[(String, String)]
    ): Validation[String, CreateResponse] =
        val m = ParamMap(params)

        def validatedContent =
            Validation.validateWith(m.nonEmpty("transId"), m.nonEmpty("redirect"))(
                CreateResponse.apply
            )

        for
            codeStr <- m.required("code")
            code <- Validation(codeStr.toInt).mapError(t =>
                s"neplatný formát 'code', není integer: $codeStr - ${t.getMessage}"
            )
            message <- m.required("message")
            result <-
                if code == 0 then validatedContent
                else Validation.fail(s"chybová odpověď API [$code]: $message")
        yield result
        end for
    end fromParams
end CreateResponse

case class ComGateResponse[T <: SuccessResponse](
    response: Either[ErrorResponse, T]
)

object ComGateResponse:
    implicit def decoder[T <: SuccessResponse](implicit
        successDecoder: JsonDecoder[T]
    ): JsonDecoder[ComGateResponse[T]] =
        implicitly[JsonDecoder[ErrorResponse]]
            .orElseEither(successDecoder)
            .map(ComGateResponse.apply)
end ComGateResponse

sealed trait PaymentStatus

object PaymentStatus:
    object Paid extends PaymentStatus
    object Cancelled extends PaymentStatus
    object Authorized extends PaymentStatus

    def fromParam(v: String): Validation[String, PaymentStatus] = v match
        case "PAID"       => Validation.succeed(Paid)
        case "CANCELLED"  => Validation.succeed(Cancelled)
        case "AUTHORIZED" => Validation.succeed(Authorized)
        case _ =>
            Validation.fail(
                s"Neznámý status platby '$v', očekávál jsem 'PAID', 'CANCELLED' or 'AUTHORIZED'"
            )
end PaymentStatus

/*
 * Payment result as received from ComGate.
 *
 * There are two parameters missing from this class, notably
 * `merchant` and `secret`, as these we do not need to carry around,
 * we just want to validate them during the reconstruction of the object.
 */
case class PaymentResult(
    transId: String,
    status: PaymentStatus,
    refId: String,
    test: Boolean,
    price: Double,
    label: String,
    curr: String,
    email: String,
    payerId: Option[String] = None,
    payerName: Option[String] = None,
    payerAcc: Option[String] = None,
    method: Option[String] = None,
    account: Option[String] = None,
    phone: Option[String] = None,
    name: Option[String] = None,
    fee: Option[String] = None
)

object PaymentResult:
    def fromParams(
        params: Seq[(String, String)]
    )(implicit config: ComGateConfig): Validation[String, PaymentResult] =
        val m = ParamMap(params)

        // Validate the merchant and secret
        val configValidation =
            for

                _ <- m
                    .boolean("test")
                    .flatMap(t =>
                        Validation.fromPredicateWith(
                            s"Přijata notifikace do ${
                                    if config.test then "testovacího"
                                    else "produkčního"
                                } prostředí s testovacím příznakem nastaveným na $t"
                        )(t)(_ == config.test)
                    )
                received <- Validation.validateWith(
                    m.nonEmpty("merchant"),
                    m.nonEmpty("secret")
                )(_ -> _)
                _ <-
                    if (config.matchCredentials).tupled(received) then Validation.unit
                    else Validation.fail("Přístupové údaje se neshodují.")
            yield ()

        for
            _ <- configValidation
            result <- Validation.validateWith(
                m.nonEmpty("transId"),
                m.nonEmpty("status").flatMap(PaymentStatus.fromParam),
                m.nonEmpty("refId"),
                m.boolean("test"),
                m.int("price").map(i => i.toDouble / 100.0),
                m.nonEmpty("label"),
                m.nonEmpty("curr"),
                m.nonEmpty("email"),
                m.optional("payerId"),
                m.optional("payerName"),
                m.optional("payerAcc"),
                m.optional("method"),
                m.optional("account"),
                m.optional("phone"),
                m.optional("name"),
                m.optional("fee")
            )(PaymentResult.apply)
        yield result
        end for
    end fromParams
end PaymentResult
