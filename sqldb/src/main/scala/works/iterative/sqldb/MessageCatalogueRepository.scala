// PURPOSE: Repository trait for accessing message catalogue data from SQL databases
// PURPOSE: Provides methods for retrieving and inserting message catalogue entities

package works.iterative.sqldb

import zio.*
import works.iterative.core.Language

trait MessageCatalogueRepository:
  /** Retrieves all message catalogue entries for a specific language.
    * Used for initial load and reload of message catalogue from database.
    *
    * @param language The language code to filter messages by
    * @return A Task containing sequence of message catalogue data for the specified language
    */
  def getAllForLanguage(language: Language): Task[Seq[MessageCatalogueData]]

  /** Inserts multiple message catalogue entities in a single transaction.
    * Used for migration from JSON files to database.
    * All inserts succeed or all fail (all-or-nothing transaction).
    * Transaction will rollback on any failure including constraint violations.
    *
    * @param entities The sequence of message catalogue data to insert
    * @return A Task that completes when all entities are inserted
    */
  def bulkInsert(entities: Seq[MessageCatalogueData]): Task[Unit]
end MessageCatalogueRepository
