package works.iterative.tapir

import zio.json.*

sealed trait ServerError
case class InternalServerError(msg: String) extends ServerError
object InternalServerError:
  def fromThrowable(t: Throwable): ServerError = InternalServerError(
    t.getMessage
  )

object ServerError:
  given JsonCodec[ServerError] = DeriveJsonCodec.gen
