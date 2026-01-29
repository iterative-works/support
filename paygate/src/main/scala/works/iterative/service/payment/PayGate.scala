package works.iterative.service.payment

import zio.*

case class PaymentInfo(
    name: String,
    price: Double,
    vs: String,
    email: String
)

case class Created(id: String, redirect: String)

case class Processed(
    paymentId: String,
    orderId: String,
    amount: Double,
    success: Boolean
)

/** A PayGate service abstraction.
  *
  * Service responsible for isolating the rest of the application from provider-specific info, eg.
  * pay gate adapter.
  */
trait PayGate:

    /** Check that the remote service is available and the access is working.
      *
      * Not expected to be called regularly, used mainly for tests or health checks maybe. Should do
      * something simple, unobtrusive and read only on the pay gate side, while still validating the
      * configuration parameters, eg. authorization and such.
      */
    def check: UIO[Unit]

    /** Create the payment using API
      */
    def create(info: PaymentInfo): UIO[Created]

    def handleNotify(result: Seq[(String, String)]): UIO[Processed]
end PayGate

object PayGate:
    def check: URIO[PayGate, Unit] = ZIO.serviceWithZIO[PayGate](_.check)
    def create(info: PaymentInfo): URIO[PayGate, Created] =
        ZIO.serviceWithZIO[PayGate](_.create(info))
    def handleNotify(
        result: Seq[(String, String)]
    ): URIO[PayGate, Processed] =
        ZIO.serviceWithZIO[PayGate](_.handleNotify(result))
end PayGate
