package works.iterative.sqldb
package testing

import zio.*
import com.dimafeng.testcontainers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import javax.sql.DataSource
import com.augustnagro.magnum.magzio.*

object PostgreSQLLayers:

    private val postgresImage = DockerImageName.parse("postgres:17-alpine")

    // Define a layer for test container
    val postgresContainer =
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
    val dataSourceLayer =
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

    // Create a Transactor from the DataSource
    val transactorLayer =
        dataSourceLayer.flatMap(env => Transactor.layer(env.get[DataSource]))

    // Create PostgreSQLDataSource from DataSource for Flyway
    val postgreSQLDataSourceLayer = dataSourceLayer >>> ZLayer {
        ZIO.service[DataSource].map(ds => PostgreSQLDataSource(ds))
    }

    // Create a custom Flyway config that allows cleaning the database
    val testFlywayConfig = FlywayConfig(
        locations = FlywayConfig.DefaultLocation :: Nil,
        cleanDisabled = false
    )

    // Create FlywayMigrationService layer with custom config
    val flywayMigrationServiceLayer =
        postgreSQLDataSourceLayer >>> FlywayMigrationService.layerWithConfig(testFlywayConfig)

end PostgreSQLLayers
