package works.iterative.akka

import zio.*
import akka.Done
import akka.actor.typed.ActorSystem
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.Offset
import akka.projection.ProjectionId
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.scaladsl.SourceProvider
import akka.projection.slick.{SlickHandler, SlickProjection}
import slick.basic.DatabaseConfig
import slick.jdbc.MySQLProfile
import akka.projection.ProjectionBehavior
import akka.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import works.iterative.entity.ViewProcessor

object AkkaProjectionSupport:
    def runSingle[E, J](
        projectionName: String,
        projectionKey: String,
        tag: String,
        processor: ViewProcessor[E],
        transform: J => E
    ): ZIO[AkkaActorSystem, Throwable, Unit] =
        ZIO.serviceWith[AkkaActorSystem](_.system).flatMap { system =>
            given ActorSystem[?] = system
            for
                given Runtime[Any] <- ZIO.runtime[Any]
                sourceProvider <- ZIO
                    .attempt[SourceProvider[Offset, EventEnvelope[J]]] {
                        EventSourcedProvider.eventsByTag[J](
                            system,
                            readJournalPluginId = JdbcReadJournal.Identifier,
                            tag = tag
                        )
                    }
                dbConfig <- ZIO.attempt[DatabaseConfig[MySQLProfile]] {
                    DatabaseConfig.forConfig(
                        "projection.slick",
                        system.settings.config
                    )
                }
                proj <- ZIO.attempt {
                    SlickProjection.exactlyOnce(
                        projectionId = ProjectionId(projectionName, projectionKey),
                        sourceProvider,
                        dbConfig,
                        handler = () => new ProjectionHandler(dbConfig, processor, transform)
                    )
                }
                _ <- ZIO.attempt {
                    ShardedDaemonProcess(system).init[ProjectionBehavior.Command](
                        name = s"$projectionName-$projectionKey",
                        numberOfInstances = 1,
                        behaviorFactory = _ => ProjectionBehavior(proj),
                        stopMessage = ProjectionBehavior.Stop
                    )
                }
            yield ()
            end for
        }
end AkkaProjectionSupport

class ProjectionHandler[E, J](
    dbConfig: DatabaseConfig[MySQLProfile],
    processor: ViewProcessor[E],
    transform: J => E
)(using runtime: Runtime[Any])
    extends SlickHandler[EventEnvelope[J]]():
    import dbConfig.profile.api.*

    override def process(envelope: EventEnvelope[J]): DBIO[Done] =
        DBIO.from(
            Unsafe.unsafe(implicit unsafe =>
                runtime.unsafe.runToFuture(processor.process(transform(envelope.event)).as(Done))
            )
        )
end ProjectionHandler
