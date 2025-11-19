// PURPOSE: PostgreSQL-specific implementation of Flyway migration service
// PURPOSE: Configures Flyway with PostgreSQL data source for database migrations

package works.iterative.sqldb.postgresql

import zio.*
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import works.iterative.sqldb.{FlywayMigrationService, FlywayConfig}

class PostgreSQLFlywayMigrationService(
    dataSource: PostgreSQLDataSource,
    config: FlywayConfig
) extends FlywayMigrationService:

    private val flyway =
        // Configure Flyway with datasource and migration locations
        val flywayConfig = Flyway.configure()
            .dataSource(dataSource.dataSource)
            .locations(config.locations*)
            .cleanDisabled(config.cleanDisabled)

        flywayConfig.load()
    end flyway

    override def migrate(): Task[MigrateResult] =
        ZIO.attempt(flyway.migrate())

    override def clean(): Task[Unit] =
        ZIO.attempt(flyway.clean()).unit

    override def validate(): Task[Unit] =
        ZIO.attempt(flyway.validate()).unit

    override def info(): Task[Unit] =
        ZIO.attempt(flyway.info()).unit
end PostgreSQLFlywayMigrationService

object PostgreSQLFlywayMigrationService:
    /** Creates a layer with default Flyway configuration
      */
    val layer: ZLayer[PostgreSQLDataSource, Throwable, FlywayMigrationService] =
        ZLayer.fromFunction { (ds: PostgreSQLDataSource) =>
            new PostgreSQLFlywayMigrationService(ds, FlywayConfig.default)
        }

    /** Creates a layer with custom Flyway configuration
      */
    def layerWithConfig(
        config: FlywayConfig
    ): ZLayer[PostgreSQLDataSource, Throwable, FlywayMigrationService] =
        ZLayer.fromFunction { (ds: PostgreSQLDataSource) =>
            new PostgreSQLFlywayMigrationService(ds, config)
        }
end PostgreSQLFlywayMigrationService
