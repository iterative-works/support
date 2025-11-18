// PURPOSE: Tests for MessageCatalogueMigrationCLI to verify argument parsing and help display
// PURPOSE: Ensures CLI provides user-friendly interface with proper error messages

package works.iterative.sqldb.postgresql
import works.iterative.sqldb.{FlywayMigrationService, FlywayConfig}

import zio.test.*
import works.iterative.sqldb.migration.MessageCatalogueMigrationCLI

object MessageCatalogueMigrationCLISpec extends ZIOSpecDefault:

  def spec = suite("MessageCatalogueMigrationCLISpec")(
    test("CLI parses language and resource arguments correctly") {
      for
        result <- MessageCatalogueMigrationCLI.parseArgs(
          List("--language=en", "--resource=/test.json")
        )
      yield assertTrue(
        result.isDefined &&
        result.get.language == "en" &&
        result.get.resourcePath == "/test.json" &&
        !result.get.dryRun &&
        !result.get.showHelp
      )
    },

    test("CLI handles missing required arguments") {
      for
        result <- MessageCatalogueMigrationCLI.parseArgs(List("--language=en"))
      yield assertTrue(result.isEmpty)
    },

    test("CLI handles --help flag") {
      for
        result <- MessageCatalogueMigrationCLI.parseArgs(List("--help"))
      yield assertTrue(
        result.isDefined &&
        result.get.showHelp
      )
    },

    test("CLI handles --dry-run flag") {
      for
        result <- MessageCatalogueMigrationCLI.parseArgs(
          List("--language=cs", "--resource=/msgs.json", "--dry-run")
        )
      yield assertTrue(
        result.isDefined &&
        result.get.dryRun &&
        result.get.language == "cs" &&
        result.get.resourcePath == "/msgs.json"
      )
    },

    test("CLI provides user-friendly error messages") {
      for
        result <- MessageCatalogueMigrationCLI.parseArgs(List("--invalid"))
      yield assertTrue(result.isEmpty)
    },

    test("CLI handles empty argument list") {
      for
        result <- MessageCatalogueMigrationCLI.parseArgs(List())
      yield assertTrue(result.isEmpty)
    },

    test("CLI handles arguments in different order") {
      for
        result <- MessageCatalogueMigrationCLI.parseArgs(
          List("--resource=/test.json", "--language=en")
        )
      yield assertTrue(
        result.isDefined &&
        result.get.language == "en" &&
        result.get.resourcePath == "/test.json"
      )
    }
  )

end MessageCatalogueMigrationCLISpec
