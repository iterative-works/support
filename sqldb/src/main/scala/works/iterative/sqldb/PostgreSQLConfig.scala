package works.iterative.sqldb
import zio.*

/** Configuration for PostgreSQL database connection
  *
  * This case class holds the configuration parameters needed to connect to a PostgreSQL database.
  *
  * Classification: Infrastructure Configuration
  */
case class PostgreSQLConfig(jdbcUrl: String, username: String, password: String)

object PostgreSQLConfig:
    val config: Config[PostgreSQLConfig] =
        import Config.*
        (string("url") zip string("username") zip string("password")).nested("pg").map(
            PostgreSQLConfig.apply
        )
    end config
end PostgreSQLConfig
