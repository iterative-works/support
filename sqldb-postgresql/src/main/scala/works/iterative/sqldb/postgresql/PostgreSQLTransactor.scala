package works.iterative.sqldb.postgresql

import zio.*
import com.augustnagro.magnum.magzio.*

/** Provides a shared Magnum Transactor for PostgreSQL database operations
  *
  * This class wraps a Magnum SQL Transactor for performing database operations.
  *
  * Classification: Infrastructure Configuration
  */
class PostgreSQLTransactor(val transactor: Transactor)

object PostgreSQLTransactor:
    val layer: ZLayer[PostgreSQLDataSource & Scope, Throwable, Transactor] =
        ZLayer.service[PostgreSQLDataSource].flatMap { env =>
            Transactor.layer(env.get[PostgreSQLDataSource].dataSource)
        }

    val managedLayer: ZLayer[PostgreSQLDataSource & Scope, Throwable, PostgreSQLTransactor] =
        layer >>> ZLayer.fromFunction(PostgreSQLTransactor.apply)
end PostgreSQLTransactor
