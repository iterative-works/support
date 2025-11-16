package works.iterative.tapir

import zio.*
import sttp.tapir.Endpoint
import sttp.capabilities.WebSockets
import sttp.capabilities.zio.ZioStreams
import works.iterative.core.auth.AccessToken
import works.iterative.core.auth.AuthenticationError
import works.iterative.core.auth.service.AuthenticationService

/** Create effectful methods to perform the endpoint operation.
  *
  * The factory takes an endpoint with correct type signature, and returns a
  * function that can call the endpoint.
  *
  * The resulting error channel is whatever the endpoint declares as the client
  * error channel, eg. the type E of ApiError[E], which is what is reported in
  * RequestFailure[E].
  *
  * The other options - AuthenticationFailure, ServerFailure - are converted to
  * defects to be handled at another level.
  *
  * This way the client can deal only with what it can actually do something
  * about.
  */
trait ApiClientFactory:
  def make[I, E, O](
      endpoint: Endpoint[
        AccessToken,
        I,
        ApiError[E],
        O,
        ZioStreams & WebSockets
      ]
  )(using
      b: BaseUriExtractor[O],
      e: ClientErrorConstructor[E]
  ): I => IO[e.Error, O]

class AuthenticatedApiClientFactory(
    authentication: AuthenticationService,
    clientFactory: ClientEndpointFactory
) extends ApiClientFactory:
  def make[I, E, O](
      endpoint: Endpoint[
        AccessToken,
        I,
        ApiError[E],
        O,
        ZioStreams & WebSockets
      ]
  )(using
      b: BaseUriExtractor[O],
      e: ClientErrorConstructor[E]
  ): I => IO[e.Error, O] =
    val client: AccessToken => I => IO[ApiError[E], O] = clientFactory.make(
      endpoint
    )(using
      b,
      ClientErrorConstructor.errorConstructor[ApiError[E]],
      ClientResultConstructor.secureResultConstructor
    )
    input =>
      authentication.currentAccessToken.flatMap {
        case Some(token) =>
          e.mapErrorCause(client(token)(input).mapErrorCause[E] {
            _.flatMap {
              case ApiError.RequestFailure(error) => Cause.fail(error)
              case ApiError.AuthFailure(error)    => Cause.die(error)
            }
          })
        case None => ZIO.die(AuthenticationError.missingUser)
      }

object ApiClientFactory:
  def layer: URLayer[
    ClientEndpointFactory & AuthenticationService,
    ApiClientFactory
  ] = ZLayer.derive[AuthenticatedApiClientFactory]
