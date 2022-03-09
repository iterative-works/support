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

class ProofProjection(system: ActorSystem[_]):

  val sourceProvider: SourceProvider[Offset, EventEnvelope[ProofEvent]] =
    EventSourcedProvider.eventsByTag[ProofEvent](
      system,
      readJournalPluginId = JdbcReadJournal.Identifier,
      tag = ProofEvent.Tag
    )

class ProofProjectionHandler(
    tag: String,
    system: ActorSystem[_],
    runtime: Runtime[ProofRepositoryWrite]
) extends Handler[EventEnvelope[ProofEvent]]():
  override def process(envelope: EventEnvelope[ProofEvent]): Future[Done] =
    val prog: RIO[ProofRepositoryWrite, Done] = envelope.event.event match
      case ev: ProofCreated => createProof(envelope.event)
      case ev               => updateProof(envelope.event)
    runtime.unsafeRunToFuture(prog)

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
