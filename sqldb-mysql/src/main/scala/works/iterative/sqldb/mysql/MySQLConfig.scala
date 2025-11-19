package works.iterative.sqldb.mysql
import zio.*

/** Configuration for MySQL database connection
  *
  * This case class holds the configuration parameters needed to connect to a MySQL database.
  *
  * Classification: Infrastructure Configuration
  */
case class MySQLConfig(jdbcUrl: String, username: String, password: String)

object MySQLConfig:
    val config: Config[MySQLConfig] =
        import Config.*
        (string("url") zip string("username") zip string("password")).nested("mysql").map(
            MySQLConfig.apply
        )
    end config
end MySQLConfig
