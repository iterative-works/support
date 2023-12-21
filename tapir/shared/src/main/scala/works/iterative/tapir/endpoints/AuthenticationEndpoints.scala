package works.iterative.tapir
package endpoints

import works.iterative.tapir.codecs.Codecs.given
import works.iterative.core.auth.AuthedUserInfo
import CustomTapir.*

trait AuthenticationEndpoints(base: BaseEndpoint):

    val currentUser: Endpoint[Unit, Unit, Unit, Option[AuthedUserInfo], Any] =
        base.get
            .in("user" / "me")
            .out(jsonBody[Option[AuthedUserInfo]])
end AuthenticationEndpoints
