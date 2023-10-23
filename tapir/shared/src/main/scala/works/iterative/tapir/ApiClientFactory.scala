package works.iterative.tapir

import zio.*
import sttp.tapir.Endpoint
import sttp.capabilities.WebSockets
import sttp.capabilities.zio.ZioStreams
import works.iterative.core.auth.AccessToken
import works.iterative.core.auth.service.*

trait ApiClientFactory:
  // TODO: Handle all authentication errors here, make sure that we remove them from the type system
  // Authentication errors do not seem to be defects.
  def make[I, E, O](
      endpoint: Endpoint[
        AccessToken,
        I,
        E,
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
        E,
        O,
        ZioStreams & WebSockets
      ]
  )(using
      b: BaseUriExtractor[O],
      e: ClientErrorConstructor[E]
  ): I => IO[e.Error, O] =
    val client: AccessToken => I => IO[e.Error, O] = clientFactory.make(
      endpoint
    )(using b, e, ClientResultConstructor.secureResultConstructor)
    input =>
      authentication.currentAccessToken.flatMap {
        case Some(token) => client(token)(input)
        case None        => ZIO.die(AuthenticationError.NotLoggedIn)
      }

object ApiClientFactory:
  val layer: URLayer[ClientEndpointFactory, ApiClientFactory] =
    ZLayer {
      for factory <- ZIO.service[ClientEndpointFactory]
      yield AuthenticatedApiClientFactory(Authentication, factory)
    }

  def withAuthentication: URLayer[
    ClientEndpointFactory & AuthenticationService,
    ApiClientFactory
  ] = ZLayer.derive[AuthenticatedApiClientFactory]
