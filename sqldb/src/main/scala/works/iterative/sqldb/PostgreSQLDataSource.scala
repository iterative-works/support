package works.iterative.sqldb

import zio.*
import javax.sql.DataSource
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}

/** Provides a shared DataSource for PostgreSQL connections
  *
  * This class manages a connection pool for PostgreSQL using HikariCP.
  *
  * Classification: Infrastructure Configuration
  */
class PostgreSQLDataSource(val dataSource: DataSource)

object PostgreSQLDataSource:
    def initDataSource(config: PostgreSQLConfig): ZIO[Scope, Throwable, HikariDataSource] =
        for
            _ <- ZIO.attempt(Class.forName("org.postgresql.Driver"))
            dataSource <- ZIO.acquireRelease(ZIO.attempt {
                val conf = HikariConfig()
                // TODO: use configurable properties
                conf.setJdbcUrl(config.jdbcUrl)
                conf.setUsername(config.username)
                conf.setPassword(config.password)
                conf.setMaximumPoolSize(10)
                conf.setMinimumIdle(5)
                conf.setConnectionTimeout(30000)
                conf.setIdleTimeout(600000)
                conf.setMaxLifetime(1800000)
                conf.setInitializationFailTimeout(-1)
                HikariDataSource(conf)
            })(ds => ZIO.attempt(ds.close()).ignore)
        yield dataSource

    val layer: ZLayer[Scope, Throwable, DataSource] = ZLayer:
        for
            config <- ZIO.config[PostgreSQLConfig](PostgreSQLConfig.config)
            dataSource <- initDataSource(config)
        yield dataSource

    def layerWithConfig(config: PostgreSQLConfig): ZLayer[Scope, Throwable, DataSource] =
        ZLayer(initDataSource(config))

    val managedLayer: ZLayer[Scope, Throwable, PostgreSQLDataSource] =
        layer >>> ZLayer.fromFunction(PostgreSQLDataSource.apply)

    def managedLayerWithConfig(config: PostgreSQLConfig)
        : ZLayer[Scope, Throwable, PostgreSQLDataSource] =
        layerWithConfig(config) >>> ZLayer.fromFunction(PostgreSQLDataSource.apply)

    val layerFromDataSource: ZLayer[DataSource, Nothing, PostgreSQLDataSource] = ZLayer {
        for
            ds <- ZIO.service[DataSource]
        yield PostgreSQLDataSource(ds)
    }

end PostgreSQLDataSource
