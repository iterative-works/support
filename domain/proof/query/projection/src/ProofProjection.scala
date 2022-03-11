package mdr.pdb
package proof
package query.projection

import akka.projection.scaladsl.SourceProvider
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.persistence.query.Offset
import akka.projection.eventsourced.EventEnvelope
import akka.actor.typed.ActorSystem
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import mdr.pdb.proof.query.repo.ProofRepository
import mdr.pdb.proof.query.repo.ProofRepositoryWrite
import akka.projection.scaladsl.Handler
import akka.Done
import scala.concurrent.Future

import zio.*
import fiftyforms.akka.UnhandledEvent
import akka.projection.jdbc.scaladsl.JdbcHandler
import akka.projection.ProjectionId
import akka.projection.slick.SlickProjection
import akka.projection.slick.SlickHandler
import slick.basic.DatabaseConfig
import slick.jdbc.MySQLProfile
import akka.projection.Projection

object ProofProjection:

  val live: ZLayer[ActorSystem[
    ?
  ] & ProofRepositoryWrite, Throwable, ProofProjection] =
    (for
      given ActorSystem[?] <- ZIO.service[ActorSystem[?]]
      runtime <- ZIO.runtime[ProofRepositoryWrite]
      sourceProvider <- Task
        .attempt[SourceProvider[Offset, EventEnvelope[ProofEvent]]] {
          EventSourcedProvider.eventsByTag[ProofEvent](
            summon[ActorSystem[?]],
            readJournalPluginId = JdbcReadJournal.Identifier,
            tag = ProofEvent.Tag
          )
        }
      dbConfig <- Task.attempt[DatabaseConfig[MySQLProfile]] {
        DatabaseConfig.forConfig(
          "projection.slick",
          summon[ActorSystem[?]].settings.config
        )
      }
      proj <- Task.attempt {
        SlickProjection.exactlyOnce(
          projectionId = ProjectionId("Proof", "proof"),
          sourceProvider,
          dbConfig,
          handler = () => new ProofProjectionHandler(dbConfig, runtime)
        )
      }
    yield ProofProjection(proj)).toLayer

case class ProofProjection(projection: Projection[EventEnvelope[ProofEvent]])

// TODO: extract TaskHandler with ZIO convertible to DBIO given runtime
class ProofProjectionHandler(
    dbConfig: DatabaseConfig[MySQLProfile],
    runtime: Runtime[ProofRepositoryWrite]
) extends SlickHandler[EventEnvelope[ProofEvent]]():
  import dbConfig.profile.api.*

  override def process(envelope: EventEnvelope[ProofEvent]): DBIO[Done] =
    val prog: RIO[ProofRepositoryWrite, Done] = envelope.event.event match
      case ev: ProofCreated => createProof(envelope.event)
      case ev               => updateProof(envelope.event)
    // TODO: is there a better way?
    DBIO.from(runtime.unsafeRunToFuture(prog))

  private def createProof(ev: ProofEvent): RIO[ProofRepositoryWrite, Done] =
    for
      proof <- handleProof(None)(ev)
      _ <- ProofRepositoryWrite.put(proof)
    yield Done

  private def updateProof(ev: ProofEvent): RIO[ProofRepositoryWrite, Done] =
    for
      orig <- ProofRepositoryWrite
        .matching(ProofRepository.WithId(ev.event.id))
        .map(_.headOption)
      proof <- handleProof(orig)(ev)
      _ <- ProofRepositoryWrite.put(proof)
    yield Done

  private def handleProof(proof: Option[Proof])(ev: ProofEvent): Task[Proof] =
    import ProofEventHandler.*
    ZIO
      .fromOption(proof.handleEvent(ev))
      .mapError(_ => UnhandledEvent(ev, proof))
