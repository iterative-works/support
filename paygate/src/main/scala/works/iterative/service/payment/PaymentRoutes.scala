package works.iterative.service.payment

import zio._
import sttp.tapir.ztapir._

trait PaymentRoutes {
  type Env <: PaymentService

  def handleError[R, T](z: ZIO[R, Throwable, T]): ZIO[R, String, T] =
    z.mapError(_.getMessage)

  val paymentServerEndpoint: ZServerEndpoint[Env, Any] =
    PaymentEndpoints.payment.zServerLogic { paymentId =>
      handleError(PaymentService.paymentRedirect(paymentId))
    }

  val resultEndpoint: ZServerEndpoint[Env, Any] =
    PaymentEndpoints.result.zServerLogic { paymentId =>
      handleError(PaymentService.redirectUrl(paymentId))
    }

  val notifyEndpoint: ZServerEndpoint[Env, Any] =
    PaymentEndpoints.status.zServerLogic { body =>
      handleError(PaymentService.handleNotify(body))
    }

  val routes: List[ZServerEndpoint[Env, Any]] =
    List(paymentServerEndpoint, notifyEndpoint, resultEndpoint)
}
