package works.iterative.sqldb

import zio.*
import org.flywaydb.core.api.output.MigrateResult

/** Service for managing database migrations using Flyway
  */
trait FlywayMigrationService:
    def migrate(): Task[MigrateResult]
    def clean(): Task[Unit]
    def validate(): Task[Unit]
    def info(): Task[Unit]
end FlywayMigrationService

/** Configuration for Flyway migrations
  *
  * @param locations
  *   Classpath locations to scan for migrations
  * @param cleanDisabled
  *   Whether database cleaning is disabled (true by default for safety)
  */
case class FlywayConfig(
    locations: List[String] = List(FlywayConfig.DefaultLocation),
    cleanDisabled: Boolean = true
)

object FlywayConfig:
    /** Default location for migrations */
    val DefaultLocation = "classpath:db/migration"

    /** Default config with standard locations */
    val default: FlywayConfig = FlywayConfig()
end FlywayConfig

object FlywayMigrationService:
    def migrate: ZIO[FlywayMigrationService, Throwable, MigrateResult] =
        ZIO.serviceWithZIO[FlywayMigrationService](_.migrate())

    def clean: ZIO[FlywayMigrationService, Throwable, Unit] =
        ZIO.serviceWithZIO[FlywayMigrationService](_.clean())

    def validate: ZIO[FlywayMigrationService, Throwable, Unit] =
        ZIO.serviceWithZIO[FlywayMigrationService](_.validate())

    def info: ZIO[FlywayMigrationService, Throwable, Unit] =
        ZIO.serviceWithZIO[FlywayMigrationService](_.info())
end FlywayMigrationService
