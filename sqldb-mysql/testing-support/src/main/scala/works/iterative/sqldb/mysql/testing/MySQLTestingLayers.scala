package works.iterative.sqldb.mysql.testing

import zio.*
import com.dimafeng.testcontainers.MySQLContainer
import org.testcontainers.utility.DockerImageName
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import javax.sql.DataSource
import com.augustnagro.magnum.magzio.*
import works.iterative.sqldb.{FlywayConfig, FlywayMigrationService}
import works.iterative.sqldb.mysql.{MySQLDataSource, MySQLTransactor, MySQLFlywayMigrationService}

object MySQLTestingLayers:

    private val mysqlImage = DockerImageName.parse("mysql:8.0")

    // Define a layer for test container
    val mysqlContainer: ZLayer[Scope, Throwable, MySQLContainer] =
        ZLayer {
            ZIO.acquireRelease {
                ZIO.attempt {
                    val container = MySQLContainer.Def(dockerImageName = mysqlImage).start()
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
        mysqlContainer >>> ZLayer.fromZIO {
            for
                container <- ZIO.service[MySQLContainer]
                _ <- ZIO.attempt(Class.forName("com.mysql.cj.jdbc.Driver"))
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

    val mySQLDataSourceLayer: ZLayer[Scope, Throwable, MySQLDataSource] =
        dataSourceLayer >>> MySQLDataSource.layerFromDataSource

    // Create a Transactor from the DataSource
    val transactorLayer: ZLayer[Scope, Throwable, Transactor] =
        dataSourceLayer.flatMap(env => Transactor.layer(env.get[DataSource]))

    val mySQLTransactorLayer
        : ZLayer[Scope, Throwable, MySQLDataSource & MySQLTransactor] =
        mySQLDataSourceLayer >+> MySQLTransactor.managedLayer

    // Create a custom Flyway config that allows cleaning the database
    // MySQL-specific migration location
    val testFlywayConfig = FlywayConfig(
        locations = "classpath:db/migration/mysql" :: Nil,
        cleanDisabled = false
    )

    // Create FlywayMigrationService layer with custom config
    val flywayMigrationServiceLayer: ZLayer[
        Scope,
        Throwable,
        MySQLDataSource & MySQLTransactor & FlywayMigrationService
    ] =
        mySQLTransactorLayer >+> MySQLFlywayMigrationService.layerWithConfig(testFlywayConfig)

end MySQLTestingLayers
