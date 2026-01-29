package works.iterative.sqldb.postgresql.testing

import zio.*
import com.dimafeng.testcontainers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import javax.sql.DataSource
import com.augustnagro.magnum.magzio.*
import works.iterative.sqldb.{FlywayConfig, FlywayMigrationService}
import works.iterative.sqldb.postgresql.{
    PostgreSQLDataSource,
    PostgreSQLTransactor,
    PostgreSQLFlywayMigrationService
}

object PostgreSQLTestingLayers:

    private val postgresImage = DockerImageName.parse("postgres:17-alpine")

    // Define a layer for test container
    val postgresContainer: ZLayer[Scope, Throwable, PostgreSQLContainer] =
        ZLayer {
            ZIO.acquireRelease {
                ZIO.attempt {
                    val container = PostgreSQLContainer(postgresImage)
                    container.start()
                    container
                }
            }(container =>
                ZIO.attempt {
                    container.stop()
                }.orDie
            )
        }

    // Create a DataSource from the container
    val dataSourceLayer: ZLayer[Scope, Throwable, DataSource] =
        postgresContainer >>> ZLayer.fromZIO {
            for
                container <- ZIO.service[PostgreSQLContainer]
                _ <- ZIO.attempt(Class.forName("org.postgresql.Driver"))
                dataSource <- ZIO.acquireRelease(ZIO.attempt {
                    val config = new HikariConfig()
                    config.setJdbcUrl(container.jdbcUrl)
                    config.setUsername(container.username)
                    config.setPassword(container.password)
                    config.setMaximumPoolSize(5)
                    new HikariDataSource(config)
                })(dataSource => ZIO.attempt(dataSource.close()).orDie)
            yield dataSource
        }

    val postgreSQLDataSourceLayer: ZLayer[Scope, Throwable, PostgreSQLDataSource] =
        dataSourceLayer >>> PostgreSQLDataSource.layerFromDataSource

    // Create a Transactor from the DataSource
    val transactorLayer: ZLayer[Scope, Throwable, Transactor] =
        dataSourceLayer.flatMap(env => Transactor.layer(env.get[DataSource]))

    val postgreSQLTransactorLayer
        : ZLayer[Scope, Throwable, PostgreSQLDataSource & PostgreSQLTransactor] =
        postgreSQLDataSourceLayer >+> PostgreSQLTransactor.managedLayer

    // Create a custom Flyway config that allows cleaning the database
    // PostgreSQL-specific migration location
    val testFlywayConfig = FlywayConfig(
        locations = "classpath:db/migration/postgresql" :: Nil,
        cleanDisabled = false
    )

    // Create FlywayMigrationService layer with custom config
    val flywayMigrationServiceLayer: ZLayer[
        Scope,
        Throwable,
        PostgreSQLDataSource & PostgreSQLTransactor & FlywayMigrationService
    ] =
        postgreSQLTransactorLayer >+> PostgreSQLFlywayMigrationService.layerWithConfig(
            testFlywayConfig
        )

end PostgreSQLTestingLayers
