package works.iterative.service.payment

import zio.*
import zio.json.*

case class Notification(orderId: String, success: Boolean)

object Notification:
  given notificationEncoder: JsonEncoder[Notification] =
    DeriveJsonEncoder.gen[Notification]

trait PaymentService:
  // Create URL for payment
  def paymentUrl(orderId: String): UIO[String]
  // Redirect to pay gate
  def paymentRedirect(paymentId: String): UIO[String]
  // Redirect to shop
  def redirectUrl(paymentId: String): UIO[String]
  // Handle payment notification from pay gate
  def handleNotify(body: Seq[(String, String)]): UIO[Unit]

object PaymentService:
  def paymentUrl(orderId: String): URIO[PaymentService, String] =
    ZIO.serviceWithZIO[PaymentService](_.paymentUrl(orderId))
  def paymentRedirect(paymentId: String): URIO[PaymentService, String] =
    ZIO.serviceWithZIO[PaymentService](_.paymentRedirect(paymentId))
  def redirectUrl(paymentId: String): URIO[PaymentService, String] =
    ZIO.serviceWithZIO[PaymentService](_.redirectUrl(paymentId))
  def handleNotify(
      body: Seq[(String, String)]
  ): URIO[PaymentService, Unit] =
    ZIO.serviceWithZIO[PaymentService](_.handleNotify(body))
