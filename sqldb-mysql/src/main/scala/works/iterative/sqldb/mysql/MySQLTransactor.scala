package works.iterative.sqldb.mysql

import zio.*
import com.augustnagro.magnum.magzio.*

/** Provides a shared Magnum Transactor for MySQL database operations
  *
  * This class wraps a Magnum SQL Transactor for performing database operations.
  *
  * Classification: Infrastructure Configuration
  */
class MySQLTransactor(val transactor: TransactorZIO)

object MySQLTransactor:
    val layer: URLayer[MySQLDataSource, TransactorZIO] =
        ZLayer.fromZIO(ZIO.serviceWith[MySQLDataSource](_.dataSource)) >>> TransactorZIO.layer

    val managedLayer: URLayer[MySQLDataSource, MySQLTransactor] =
        layer >>> ZLayer.fromFunction(MySQLTransactor.apply)
end MySQLTransactor
