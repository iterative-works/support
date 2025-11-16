// PURPOSE: Migration tool to import message catalogue entries from JSON files to SQL database
// PURPOSE: Handles JSON parsing, batch processing, and bulk insertion of messages

package works.iterative.sqldb.migration

import zio.*
import zio.json.*
import works.iterative.core.{Language, MessageId}
import works.iterative.core.repository.MessageCatalogueRepository
import works.iterative.sqldb.MessageCatalogue

object MessageCatalogueMigration:

  /**
   * Migrates messages from a JSON resource file to the SQL database.
   *
   * @param repository The message catalogue repository for inserting messages
   * @param language The language code for all messages in the JSON file
   * @param jsonResourcePath Path to JSON resource file (e.g., "/messages_en.json")
   * @return Task that completes when migration is finished
   */
  def migrateFromJson(
    repository: MessageCatalogueRepository,
    language: Language,
    jsonResourcePath: String
  ): Task[Unit] =
    for
      // Load JSON resource
      jsonContent <- loadJsonResource(jsonResourcePath)
      // Parse JSON as Map[String, String]
      messageMap <- ZIO.fromEither(jsonContent.fromJson[Map[String, String]])
        .mapError(err => new RuntimeException(s"Failed to parse JSON: $err"))
      // Log migration start
      _ <- ZIO.logInfo(s"Starting migration of ${messageMap.size} messages from $jsonResourcePath for language $language")
      // Convert to entities
      entities = messageMap.map { case (key, text) =>
        MessageCatalogue.fromMessage(
          MessageId(key),
          language,
          text,
          Some(s"Migrated from $jsonResourcePath"),
          None
        ).toDomain
      }.toSeq
      // Log progress for large files
      _ <- ZIO.when(messageMap.size >= 100)(
        ZIO.logInfo(s"Processing large message file with ${messageMap.size} entries")
      )
      // Bulk insert all entities
      _ <- repository.bulkInsert(entities)
      // Log completion
      _ <- ZIO.logInfo(s"Successfully migrated ${messageMap.size} messages")
    yield ()

  /**
   * Loads a JSON resource file from classpath.
   *
   * @param resourcePath Path to resource (must start with /)
   * @return Task containing JSON content as string
   */
  private def loadJsonResource(resourcePath: String): Task[String] =
    ZIO.acquireReleaseWith(
      acquire = ZIO.attempt {
        val stream = getClass.getResourceAsStream(resourcePath)
        if stream == null then
          throw new RuntimeException(s"Resource not found: $resourcePath")
        scala.io.Source.fromInputStream(stream, "UTF-8")
      }
    )(
      release = source => ZIO.succeed(source.close())
    )(
      use = source => ZIO.attempt(source.mkString)
    )

end MessageCatalogueMigration
