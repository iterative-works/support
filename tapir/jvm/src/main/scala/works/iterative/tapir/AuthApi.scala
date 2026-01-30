package works.iterative.tapir

import endpoints.AuthenticationEndpoints
import zio.*
import CustomTapir.*
import works.iterative.core.auth.service.AuthenticationService

trait AuthApi(ep: AuthenticationEndpoints):
    val currentUser: ZServerEndpoint[AuthenticationService, Any] =
        ep.currentUser.zServerLogic { _ =>
            ZIO.serviceWithZIO[AuthenticationService](_.currentUserInfo)
        }
end AuthApi
