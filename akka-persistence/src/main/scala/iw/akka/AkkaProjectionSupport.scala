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
  def runSingle[E](
      projectionName: String,
      projectionKey: String,
      tag: String,
      processor: ViewProcessor[E]
  ): ZIO[AkkaActorSystem, Throwable, Unit] =
    ZIO.serviceWith[AkkaActorSystem](_.system).flatMap { system =>
      given ActorSystem[?] = system
      for
        given Runtime[Any] <- ZIO.runtime[Any]
        sourceProvider <- ZIO
          .attempt[SourceProvider[Offset, EventEnvelope[E]]] {
            EventSourcedProvider.eventsByTag[E](
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
            handler = () => new ProjectionHandler(dbConfig, processor)
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
    }

class ProjectionHandler[E](
    dbConfig: DatabaseConfig[MySQLProfile],
    processor: ViewProcessor[E]
)(using runtime: Runtime[Any])
    extends SlickHandler[EventEnvelope[E]]():
  import dbConfig.profile.api.*

  override def process(envelope: EventEnvelope[E]): DBIO[Done] =
    DBIO.from(
      Unsafe.unsafe(implicit unsafe =>
        runtime.unsafe.runToFuture(processor.process(envelope.event).as(Done))
      )
    )
