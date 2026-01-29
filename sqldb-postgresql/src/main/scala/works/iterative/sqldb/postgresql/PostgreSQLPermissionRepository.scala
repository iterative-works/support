// PURPOSE: PostgreSQL implementation of PermissionRepository using Magnum ORM
// PURPOSE: Provides database persistence for permission relation tuples with efficient queries

package works.iterative.sqldb.postgresql

import zio.*
import works.iterative.core.auth.{RelationTuple, UserId, PermissionTarget}
import works.iterative.sqldb.PermissionRepository

object PostgreSQLPermissionRepository:
    /** ZIO layer for PermissionRepository Wires PostgreSQLTransactor to
      * PostgreSQLPermissionRepositoryImpl
      */
    val layer: URLayer[PostgreSQLTransactor, PermissionRepository] =
        ZLayer.fromFunction((ts: PostgreSQLTransactor) => PostgreSQLPermissionRepositoryImpl(ts))
end PostgreSQLPermissionRepository

case class PostgreSQLPermissionRepositoryImpl(ts: PostgreSQLTransactor)
    extends PermissionRepository:
    import com.augustnagro.magnum.Repo
    import com.augustnagro.magnum.magzio.sql

    private val repo = Repo[PermissionCreator, Permissions, Long]

    override def hasRelation(
        userId: UserId,
        relation: String,
        target: PermissionTarget
    ): Task[Boolean] =
        ts.transactor.connect:
            val result = sql"""
        SELECT 1 FROM permissions
        WHERE user_id = ${userId.value}
          AND relation = $relation
          AND namespace = ${target.namespace}
          AND object_id = ${target.value}
        LIMIT 1
      """.query[Int].run()
            result.nonEmpty

    override def addRelation(
        userId: UserId,
        relation: String,
        target: PermissionTarget
    ): Task[Unit] =
        ts.transactor.transact:
            sql"""
        INSERT INTO permissions (user_id, relation, namespace, object_id, created_at)
        VALUES (${userId.value}, $relation, ${target.namespace}, ${target.value}, NOW())
        ON CONFLICT (user_id, relation, namespace, object_id) DO NOTHING
      """.update.run()
            ()

    override def removeRelation(
        userId: UserId,
        relation: String,
        target: PermissionTarget
    ): Task[Unit] =
        ts.transactor.transact:
            // Find the permission first
            val existing = sql"""
        SELECT * FROM permissions
        WHERE user_id = ${userId.value}
          AND relation = $relation
          AND namespace = ${target.namespace}
          AND object_id = ${target.value}
      """.query[Permissions].run()

            existing.headOption match
                case Some(p) if p.id.isDefined =>
                    repo.deleteById(p.id.get)
                    ()
                case _ => () // Idempotent - deleting non-existent succeeds
            end match

    override def getUserRelations(
        userId: UserId,
        namespace: String
    ): Task[Set[RelationTuple]] =
        ts.transactor.connect:
            val rows = sql"""
        SELECT * FROM permissions
        WHERE user_id = ${userId.value}
          AND namespace = $namespace
      """.query[Permissions].run()
            rows.map(_.toDomain).toSet

end PostgreSQLPermissionRepositoryImpl
