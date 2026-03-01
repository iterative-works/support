package works.iterative.sqldb.postgresql

import zio.*
import com.augustnagro.magnum.magzio.*

/** Provides a shared Magnum Transactor for PostgreSQL database operations
  *
  * This class wraps a Magnum SQL Transactor for performing database operations.
  *
  * Classification: Infrastructure Configuration
  */
class PostgreSQLTransactor(val transactor: TransactorZIO)

object PostgreSQLTransactor:
    val layer: URLayer[PostgreSQLDataSource, TransactorZIO] =
        ZLayer.fromZIO(ZIO.serviceWith[PostgreSQLDataSource](_.dataSource)) >>> TransactorZIO.layer

    val managedLayer: URLayer[PostgreSQLDataSource, PostgreSQLTransactor] =
        layer >>> ZLayer.fromFunction(PostgreSQLTransactor.apply)
end PostgreSQLTransactor
