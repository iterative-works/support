// PURPOSE: Repository trait for accessing message catalogue data from the database
// PURPOSE: Provides methods for retrieving and inserting message catalogue entities

package works.iterative.sqldb

import zio.*
import works.iterative.core.Language

trait MessageCatalogueRepository:
  /** Retrieves all message catalogue entries for a specific language.
    * Used for initial load and reload of message catalogue from database.
    *
    * @param language The language code to filter messages by
    * @return A Task containing sequence of message catalogue entities for the specified language
    */
  def getAllForLanguage(language: Language): Task[Seq[MessageCatalogue]]

  /** Inserts multiple message catalogue entities in a single transaction.
    * Used for migration from JSON files to database.
    * All inserts succeed or all fail (all-or-nothing transaction).
    * Transaction will rollback on any failure including constraint violations.
    *
    * @param entities The sequence of message catalogue entities to insert
    * @return A Task that completes when all entities are inserted
    */
  def bulkInsert(entities: Seq[MessageCatalogue]): Task[Unit]
end MessageCatalogueRepository

object MessageCatalogueRepository:
  /** ZIO layer for MessageCatalogueRepository
    * Wires PostgreSQLTransactor to MessageCatalogueRepositoryImpl
    */
  val layer: URLayer[PostgreSQLTransactor, MessageCatalogueRepository] =
    ZLayer.fromFunction((ts: PostgreSQLTransactor) => MessageCatalogueRepositoryImpl(ts))
end MessageCatalogueRepository

case class MessageCatalogueRepositoryImpl(ts: PostgreSQLTransactor) extends MessageCatalogueRepository:
  import com.augustnagro.magnum.Repo
  import com.augustnagro.magnum.magzio.{sql, given}

  private val repo = Repo[MessageCatalogueCreator, MessageCatalogue, Long]

  override def getAllForLanguage(language: Language): Task[Seq[MessageCatalogue]] =
    ts.transactor.connect:
      sql"SELECT * FROM message_catalogue WHERE language = $language"
        .query[MessageCatalogue]
        .run()

  override def bulkInsert(entities: Seq[MessageCatalogue]): Task[Unit] =
    ZIO.logInfo(s"Bulk inserting ${entities.size} messages") *>
      ts.transactor.transact:
        val creators = entities.map(MessageCatalogue.toCreator)
        repo.insertAllReturning(creators)
      .unit
end MessageCatalogueRepositoryImpl
