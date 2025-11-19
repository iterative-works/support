package works.iterative.sqldb.mysql

import zio.*
import com.augustnagro.magnum.magzio.*

/** Provides a shared Magnum Transactor for MySQL database operations
  *
  * This class wraps a Magnum SQL Transactor for performing database operations.
  *
  * Classification: Infrastructure Configuration
  */
class MySQLTransactor(val transactor: Transactor)

object MySQLTransactor:
    val layer: ZLayer[MySQLDataSource & Scope, Throwable, Transactor] =
        ZLayer.service[MySQLDataSource].flatMap { env =>
            Transactor.layer(env.get[MySQLDataSource].dataSource)
        }

    val managedLayer: ZLayer[MySQLDataSource & Scope, Throwable, MySQLTransactor] =
        layer >>> ZLayer.fromFunction(MySQLTransactor.apply)
end MySQLTransactor
