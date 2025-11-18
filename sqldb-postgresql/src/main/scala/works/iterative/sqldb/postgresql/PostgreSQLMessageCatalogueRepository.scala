// PURPOSE: Repository implementation for accessing message catalogue data from the database
// PURPOSE: Provides PostgreSQL-specific implementation with Magnum ORM

package works.iterative.sqldb.postgresql

import zio.*
import works.iterative.core.Language
import works.iterative.core.model.MessageCatalogueData
import works.iterative.core.repository.MessageCatalogueRepository

object PostgreSQLMessageCatalogueRepository:
  /** ZIO layer for MessageCatalogueRepository
    * Wires PostgreSQLTransactor to MessageCatalogueRepositoryImpl
    */
  val layer: URLayer[PostgreSQLTransactor, MessageCatalogueRepository] =
    ZLayer.fromFunction((ts: PostgreSQLTransactor) => MessageCatalogueRepositoryImpl(ts))
end PostgreSQLMessageCatalogueRepository

case class MessageCatalogueRepositoryImpl(ts: PostgreSQLTransactor) extends MessageCatalogueRepository:
  import com.augustnagro.magnum.Repo
  import com.augustnagro.magnum.magzio.sql

  private val repo = Repo[MessageCatalogueCreator, MessageCatalogue, Long]

  override def getAllForLanguage(language: Language): Task[Seq[MessageCatalogueData]] =
    ts.transactor.connect:
      sql"SELECT * FROM message_catalogue WHERE language = $language"
        .query[MessageCatalogue]
        .run()
        .map(row => row.toDomain)

  override def bulkInsert(entities: Seq[MessageCatalogueData]): Task[Unit] =
    ZIO.logInfo(s"Bulk inserting ${entities.size} messages") *>
      ts.transactor.transact:
        val creators = entities.map(MessageCatalogue.fromDomain)
        repo.insertAllReturning(creators)
      .unit
end MessageCatalogueRepositoryImpl
