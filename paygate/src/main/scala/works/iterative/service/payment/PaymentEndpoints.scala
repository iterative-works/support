package works.iterative.service.payment

import works.iterative.tapir.CustomTapir.*
import sttp.model.StatusCode
import sttp.model.Method

object PaymentEndpoints:
    val paymentBase = "payment"

    val payment: Endpoint[Unit, String, String, String, Any] =
        endpoint.get
            .in(paymentBase / path[String]("paymentId"))
            .out(header[String]("Location"))
            .out(statusCode(StatusCode.Found))
            .errorOut(plainBody[String])

    // Návrat z platební brány
    def result: Endpoint[Unit, String, String, String, Any] =
        endpoint
            .in("result" / path[String]("paymentId"))
            .method(Method.GET)
            .out(header[String]("Location"))
            .out(statusCode(StatusCode.Found))
            .errorOut(plainBody[String])

    // Oznámení výsledku platby, ComGate specific
    // TODO: security input - validate remote IP
    val status: Endpoint[Unit, Seq[(String, String)], String, Unit, Any] =
        endpoint
            .in("notify")
            .method(Method.POST)
            .in(formBody[Seq[(String, String)]])
            .errorOut(stringBody)
            .out(statusCode(StatusCode.Ok))
end PaymentEndpoints
