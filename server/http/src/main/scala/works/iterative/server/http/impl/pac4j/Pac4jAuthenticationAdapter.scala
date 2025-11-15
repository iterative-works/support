// PURPOSE: Adapter that bridges Pac4J Java library authentication to ZIO AuthenticationService
// PURPOSE: Maps Pac4J CommonProfile to BasicProfile and manages user context via FiberRef
package works.iterative.server.http
package impl.pac4j

import zio.*
import org.pac4j.core.profile.CommonProfile
import works.iterative.core.auth.*
import works.iterative.core.auth.service.*
import works.iterative.core.*
import scala.jdk.CollectionConverters.*

/**
  * Adapter that bridges Pac4J authentication to ZIO AuthenticationService.
  *
  * This adapter:
  * - Maps Pac4J CommonProfile (Java) to BasicProfile (Scala/ZIO)
  * - Handles null values defensively with Option
  * - Stores user context in FiberRef for request-scoped authentication
  * - Extracts roles from Pac4J profile attributes
  *
  * Example usage:
  * {{{
  * // After Pac4J successful authentication with CommonProfile
  * val profile: CommonProfile = ... // from Pac4J
  * val token = AccessToken("oauth-token-123")
  *
  * for
  *   adapter <- ZIO.service[AuthenticationService]
  *   basicProfile = Pac4jAuthenticationAdapter.mapProfile(profile)
  *   _ <- adapter.loggedIn(token, basicProfile)
  *   currentUser <- adapter.currentUserInfo
  * yield currentUser // Some(AuthedUserInfo(...))
  * }}}
  */
object Pac4jAuthenticationAdapter extends AuthenticationService:
    private val currentUser: FiberRef[Option[AuthedUserInfo]] =
        Unsafe.unsafely(FiberRef.unsafe.make(None))

    override val currentUserInfo: UIO[Option[AuthedUserInfo]] = currentUser.get

    override def loggedIn(token: AccessToken, profile: BasicProfile): UIO[Unit] =
        ZIO.logDebug(s"Mapped Pac4J profile: id=${profile.subjectId.value}, name=${profile.userName}, email=${profile.email}, roles=${profile.roles.size}") *>
        ZIO.logDebug(s"User logged in: ${profile.subjectId.value}") *>
        currentUser.set(Some(AuthedUserInfo(token, profile)))

    /**
      * Maps a Pac4J CommonProfile to our BasicProfile.
      *
      * Handles null values from Java interop defensively. This is a pure function
      * that can be tested independently of ZIO effects.
      *
      * @param profile Pac4J CommonProfile (Java object, may have null values)
      * @return BasicProfile with optional fields for missing data
      * @throws IllegalArgumentException if profile ID is null or empty
      *
      * Example:
      * {{{
      * val pac4jProfile = new CommonProfile()
      * pac4jProfile.setId("user-123")
      * pac4jProfile.addAttribute("name", "John Doe")
      * pac4jProfile.addAttribute("email", "john@example.com")
      * pac4jProfile.addAttribute("roles", java.util.Arrays.asList("admin", "user"))
      *
      * val basicProfile = adapter.mapProfile(pac4jProfile)
      * // BasicProfile(UserId("user-123"), Some(UserName("John Doe")), Some(Email("john@example.com")), None, Set(UserRole("admin"), UserRole("user")))
      * }}}
      */
    def mapProfile(profile: CommonProfile): BasicProfile =
        val id = Option(profile.getId)
            .filter(_.trim.nonEmpty)
            .getOrElse(throw new IllegalArgumentException("Profile ID cannot be null or empty"))

        val name = Option(profile.getAttribute("name"))
            .map(_.toString)
            .flatMap(n => if n.trim.nonEmpty then Some(UserName.unsafe(n)) else None)

        val email = Option(profile.getAttribute("email"))
            .map(_.toString)
            .flatMap(e => if e.trim.nonEmpty then Some(Email.unsafe(e)) else None)

        val roles = extractRoles(profile)

        BasicProfile(
            subjectId = UserId.unsafe(id),
            userName = name,
            email = email,
            avatar = None,
            roles = roles
        )
    end mapProfile

    /**
      * Extracts roles from Pac4J profile attributes.
      *
      * Looks for a "roles" attribute which may be:
      * - A Java List of strings
      * - A single string
      * - null/missing
      */
    private def extractRoles(profile: CommonProfile): Set[UserRole] =
        Option(profile.getAttribute("roles")) match
            case Some(rolesAttr: java.util.List[?]) =>
                rolesAttr.asScala
                    .collect { case s: String if s.trim.nonEmpty => UserRole.unsafe(s) }
                    .toSet
            case Some(rolesAttr: String) if rolesAttr.trim.nonEmpty =>
                Set(UserRole.unsafe(rolesAttr))
            case _ =>
                Set.empty
    end extractRoles

    val layer: ZLayer[Any, Nothing, AuthenticationService] =
        ZLayer.succeed(Pac4jAuthenticationAdapter)

end Pac4jAuthenticationAdapter
