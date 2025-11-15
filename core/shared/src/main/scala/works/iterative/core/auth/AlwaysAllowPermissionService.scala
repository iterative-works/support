// PURPOSE: Permission service that always grants access - FOR TESTING/EMERGENCY USE ONLY
// PURPOSE: WARNING: Never use in production - bypasses all security controls

package works.iterative.core.auth

import zio.*

/** Permission service that unconditionally grants all permissions.
  *
  * WARNING: This implementation bypasses all security controls and should ONLY be used:
  * 1. In automated tests that don't care about authorization
  * 2. In emergency/maintenance scenarios with explicit approval
  *
  * NEVER use this in production without understanding the security implications.
  * Anyone who can authenticate will have unrestricted access to all resources.
  *
  * This service:
  * - Returns true for all isAllowed checks regardless of user/action/resource
  * - Returns empty set for listAllowed (cannot enumerate all possible resources)
  * - Logs a warning when instantiated to prevent accidental production use
  */
class AlwaysAllowPermissionService extends PermissionService:

  def logWarning: UIO[Unit] =
    val isProd =
      sys.env.get("ENV").exists(_.toLowerCase.contains("prod")) ||
      sys.env.get("ENVIRONMENT").exists(_.toLowerCase.contains("prod")) ||
      sys.env.get("APP_ENV").exists(_.toLowerCase.contains("prod"))

    for {
      _ <- ZIO.logWarning("AlwaysAllowPermissionService instantiated - ALL PERMISSIONS GRANTED")
      _ <- ZIO.logError("SECURITY: This service bypasses all authorization checks")
      _ <- ZIO.when(isProd) {
        ZIO.die(new IllegalStateException(
          "AlwaysAllowPermissionService cannot be used in production environment. " +
          "Detected production via environment variables."
        ))
      }
    } yield ()

  override def isAllowed(
      subj: Option[UserInfo],
      action: PermissionOp,
      obj: PermissionTarget
  ): UIO[Boolean] =
    ZIO.succeed(true)

  override def listAllowed(
      subj: UserInfo,
      action: PermissionOp,
      namespace: String
  ): UIO[Set[String]] =
    ZIO.succeed(Set.empty)
end AlwaysAllowPermissionService

object AlwaysAllowPermissionService:
  val layer: ZLayer[Any, Nothing, PermissionService] =
    ZLayer.fromZIO(
      for {
        service <- ZIO.succeed(AlwaysAllowPermissionService())
        _ <- service.logWarning
      } yield service
    )
end AlwaysAllowPermissionService
