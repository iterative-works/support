// PURPOSE: Database entity representing permission relation tuples in MySQL
// PURPOSE: Maps between domain model (RelationTuple) and database columns with Magnum ORM

package works.iterative.sqldb.mysql

import com.augustnagro.magnum.*
import works.iterative.core.auth.{RelationTuple, UserId, PermissionTarget}
import java.time.OffsetDateTime
import MySQLDbCodecs.given

/** Creator entity for inserting new permission tuples (without ID) */
case class PermissionCreator(
    userId: String,
    relation: String,
    namespace: String,
    objectId: String,
    createdAt: OffsetDateTime
) derives DbCodec

/** Database entity for permission relation tuples */
@Table(MySqlDbType, SqlNameMapper.CamelToSnakeCase)
case class Permissions(
    @Id id: Option[Long],
    userId: String,
    relation: String,
    namespace: String,
    objectId: String,
    createdAt: OffsetDateTime
) derives DbCodec:
    /** Converts this database row to domain model */
    def toDomain: RelationTuple =
        RelationTuple(
            user = UserId.unsafe(userId),
            relation = relation,
            target = PermissionTarget.unsafe(namespace, objectId)
        )
end Permissions

object Permissions:
    /** Converts domain model to database row for insertion */
    def fromDomain(tuple: RelationTuple): PermissionCreator =
        PermissionCreator(
            userId = tuple.user.value,
            relation = tuple.relation,
            namespace = tuple.target.namespace,
            objectId = tuple.target.value,
            createdAt = OffsetDateTime.now()
        )
end Permissions
