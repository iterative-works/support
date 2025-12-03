package works.iterative.sqldb.mysql

import zio.*
import javax.sql.DataSource
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}

/** Provides a shared DataSource for MySQL connections
  *
  * This class manages a connection pool for MySQL using HikariCP.
  *
  * Classification: Infrastructure Configuration
  */
class MySQLDataSource(val dataSource: DataSource)

object MySQLDataSource:
    def initDataSource(config: MySQLConfig): ZIO[Scope, Throwable, HikariDataSource] =
        for
            _ <- ZIO.attempt(Class.forName("com.mysql.cj.jdbc.Driver"))
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
            config <- ZIO.config[MySQLConfig](MySQLConfig.config)
            dataSource <- initDataSource(config)
        yield dataSource

    def layerWithConfig(config: MySQLConfig): ZLayer[Scope, Throwable, DataSource] =
        ZLayer(initDataSource(config))

    val managedLayer: ZLayer[Scope, Throwable, MySQLDataSource] =
        layer >>> ZLayer.fromFunction(MySQLDataSource.apply)

    def managedLayerWithConfig(config: MySQLConfig)
        : ZLayer[Scope, Throwable, MySQLDataSource] =
        layerWithConfig(config) >>> ZLayer.fromFunction(MySQLDataSource.apply)

    val layerFromDataSource: ZLayer[DataSource, Nothing, MySQLDataSource] = ZLayer {
        for
            ds <- ZIO.service[DataSource]
        yield MySQLDataSource(ds)
    }

end MySQLDataSource
