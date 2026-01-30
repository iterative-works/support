// PURPOSE: Minimal test to verify MySQL test layer works with centralized DbCodecs
// PURPOSE: Uses MySQLDbCodecs to avoid circular dependency issues

package works.iterative.sqldb.mysql

import zio.*
import zio.test.*
import zio.test.TestAspect.*
import works.iterative.sqldb.mysql.testing.MySQLTestingLayers.*
import com.augustnagro.magnum.*
import java.time.OffsetDateTime
import MySQLDbCodecs.given

// Test entity using OffsetDateTime with centralized codec
@Table(MySqlDbType, SqlNameMapper.CamelToSnakeCase)
case class TestWithOffsetDateTime(
    @Id id: Option[Long],
    name: String,
    createdAt: OffsetDateTime
) derives DbCodec

case class TestWithOffsetDateTimeCreator(
    name: String,
    createdAt: OffsetDateTime
) derives DbCodec

object MySQLMinimalSpec extends ZIOSpecDefault:

  def spec = suite("MySQLMinimalSpec")(
    // scalafix:off DisableSyntax.null
    // Test assertion: verifying repo instantiation succeeds
    test("can create Repo with OffsetDateTime (using centralized MySQLDbCodecs)") {
      for
        _ <- ZIO.logInfo("Test: about to get MySQLTransactor")
        ts <- ZIO.service[MySQLTransactor]
        _ <- ZIO.logInfo(s"Got transactor: $ts")
        _ <- ZIO.logInfo("Creating OffsetDateTime Repo...")
        repo <- ZIO.attempt {
          Repo[TestWithOffsetDateTimeCreator, TestWithOffsetDateTime, Long]
        }
        _ <- ZIO.logInfo(s"Got OffsetDateTime repo: $repo")
      yield assertTrue(repo != null)
    }
    // scalafix:on DisableSyntax.null
  ).provideSomeShared[Scope](mySQLTransactorLayer) @@ timeout(60.seconds)

end MySQLMinimalSpec
