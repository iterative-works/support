// PURPOSE: Command-line interface for migrating message catalogue from JSON files to SQL database
// PURPOSE: Parses arguments, sets up layers, and executes migration with user-friendly output

package works.iterative.sqldb.postgresql.migration

import zio.*
import works.iterative.core.Language
import works.iterative.core.repository.MessageCatalogueRepository
import works.iterative.sqldb.postgresql.*

object MessageCatalogueMigrationCLI extends ZIOAppDefault:

  case class CliConfig(
    language: String,
    resourcePath: String,
    dryRun: Boolean = false,
    showHelp: Boolean = false
  )

  def run: ZIO[ZIOAppArgs & Scope, Any, ExitCode] =
    for
      args <- ZIO.serviceWith[ZIOAppArgs](_.getArgs)
      config <- parseArgs(args.toList)
      exitCode <- config match
        case Some(cfg) if cfg.showHelp =>
          printHelp *> ZIO.succeed(ExitCode.success)
        case Some(cfg) =>
          runMigration(cfg)
        case None =>
          Console.printLineError("Invalid arguments. Use --help for usage instructions.") *>
            ZIO.succeed(ExitCode.failure)
    yield exitCode

  private[sqldb] def parseArgs(args: List[String]): UIO[Option[CliConfig]] =
    ZIO.succeed {
      val argMap = args.foldLeft(Map.empty[String, String]) { (acc, arg) =>
        if arg.startsWith("--") then
          arg.split("=", 2) match
            case Array(key, value) => acc + (key.drop(2) -> value)
            case Array(key) => acc + (key.drop(2) -> "true")
        else acc
      }

      if argMap.contains("help") then
        Some(CliConfig("", "", showHelp = true))
      else
        for
          lang <- argMap.get("language")
          resource <- argMap.get("resource")
        yield CliConfig(
          language = lang,
          resourcePath = resource,
          dryRun = argMap.get("dry-run").contains("true")
        )
    }

  private def printHelp: UIO[Unit] =
    Console.printLine(
      """Message Catalogue Migration Tool
        |
        |Usage:
        |  migration-cli --language=<lang> --resource=<path> [OPTIONS]
        |
        |Required Arguments:
        |  --language=<lang>      Language code (e.g., en, cs, de)
        |  --resource=<path>      Path to JSON resource file (e.g., /messages_en.json)
        |
        |Optional Arguments:
        |  --dry-run              Preview migration without inserting to database
        |  --help                 Show this help message
        |
        |Examples:
        |  migration-cli --language=en --resource=/messages_en.json
        |  migration-cli --language=cs --resource=/messages_cs.json --dry-run
        |""".stripMargin
    ).ignore

  private def runMigration(config: CliConfig): ZIO[Scope, Any, ExitCode] =
    val program = for
      _ <- Console.printLine(s"Starting migration for language '${config.language}' from '${config.resourcePath}'")
      _ <- if config.dryRun then
        Console.printLine("[DRY RUN] Migration preview mode - no data will be inserted")
      else
        ZIO.unit
      repository <- ZIO.service[MessageCatalogueRepository]
      lang <- ZIO.attempt(Language.unsafe(config.language))
        .mapError(e => new RuntimeException(s"Invalid language code: ${config.language}", e))
      _ <- if config.dryRun then
        Console.printLine(s"[DRY RUN] Would migrate messages from ${config.resourcePath} for language $lang")
      else
        MessageCatalogueMigration.migrateFromJson(repository, lang, config.resourcePath)
      _ <- Console.printLine("Migration completed successfully!")
    yield ExitCode.success

    program
      .provideSomeLayer[Scope](
        PostgreSQLDatabaseSupport.layer >>> MessageCatalogueRepository.layer
      )
      .catchAll { error =>
        Console.printLineError(s"Migration failed: ${error.getMessage}") *>
          ZIO.succeed(ExitCode.failure)
      }

end MessageCatalogueMigrationCLI
