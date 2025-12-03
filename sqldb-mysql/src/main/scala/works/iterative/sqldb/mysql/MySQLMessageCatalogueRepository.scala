// PURPOSE: Repository implementation for accessing message catalogue data from the database
// PURPOSE: Provides MySQL-specific implementation with Magnum ORM

package works.iterative.sqldb.mysql

import zio.*
import works.iterative.core.Language
import works.iterative.core.model.MessageCatalogueData
import works.iterative.core.repository.MessageCatalogueRepository

object MySQLMessageCatalogueRepository:
  /** ZIO layer for MessageCatalogueRepository
    * Wires MySQLTransactor to MessageCatalogueRepositoryImpl
    */
  val layer: URLayer[MySQLTransactor, MessageCatalogueRepository] =
    ZLayer.fromFunction((ts: MySQLTransactor) => MessageCatalogueRepositoryImpl(ts))
end MySQLMessageCatalogueRepository

case class MessageCatalogueRepositoryImpl(ts: MySQLTransactor) extends MessageCatalogueRepository:
  import com.augustnagro.magnum.Repo
  import com.augustnagro.magnum.magzio.sql

  private val repo = Repo[MessageCatalogueCreator, MessageCatalogue, Long]

  override def getAllForLanguage(language: Language): Task[Seq[MessageCatalogueData]] =
    ts.transactor.connect:
      sql"SELECT * FROM message_catalogue WHERE language = ${language.value}"
        .query[MessageCatalogue]
        .run()
        .map(row => row.toDomain)

  override def bulkInsert(entities: Seq[MessageCatalogueData]): Task[Unit] =
    ZIO.logInfo(s"Bulk inserting ${entities.size} messages") *>
      ts.transactor.transact:
        val creators = entities.map(MessageCatalogue.fromDomain)
        repo.insertAll(creators)
      .unit
end MessageCatalogueRepositoryImpl
