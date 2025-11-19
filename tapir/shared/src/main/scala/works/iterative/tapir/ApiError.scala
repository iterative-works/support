package works.iterative.tapir

import works.iterative.core.auth.AuthenticationError

sealed trait ApiError[+ClientError]
object ApiError:
    case class AuthFailure(error: AuthenticationError) extends ApiError[Nothing]
    case class RequestFailure[ClientError](error: ClientError)
        extends ApiError[ClientError]
