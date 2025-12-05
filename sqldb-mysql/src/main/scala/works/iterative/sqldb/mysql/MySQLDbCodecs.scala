// PURPOSE: Centralized DbCodec definitions for MySQL to avoid circular dependencies
// PURPOSE: All time-related codecs use java.sql.Timestamp as the base (native JDBC support)

package works.iterative.sqldb.mysql

import com.augustnagro.magnum.DbCodec
import java.time.{Instant, OffsetDateTime, ZoneOffset}
import java.sql.Timestamp

/** Shared DbCodecs for MySQL entities.
  *
  * These codecs use java.sql.Timestamp as the base type because:
  * 1. Timestamp has native JDBC/Magnum support
  * 2. Avoids circular dependency: Magnum's Instant codec uses OffsetDateTime,
  *    so defining OffsetDateTime in terms of Instant creates a deadlock
  */
object MySQLDbCodecs:
  /** DbCodec for OffsetDateTime using Timestamp as base */
  given DbCodec[OffsetDateTime] =
    DbCodec[Timestamp].biMap(
      ts => ts.toInstant.atOffset(ZoneOffset.UTC),
      odt => Timestamp.from(odt.toInstant)
    )

  /** DbCodec for Instant using Timestamp as base */
  given DbCodec[Instant] =
    DbCodec[Timestamp].biMap(
      ts => ts.toInstant,
      instant => Timestamp.from(instant)
    )
end MySQLDbCodecs
