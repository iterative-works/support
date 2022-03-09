package mdr.pdb
package proof
package command
package entity

import akka.persistence.typed.scaladsl.Effect
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import akka.persistence.typed.PersistenceId
import akka.actor.typed.Behavior
import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import akka.Done

import fiftyforms.akka.*
import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.cluster.sharding.typed.scaladsl.EntityContext
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.sharding.typed.scaladsl.Entity

object ProofBehavior:

  type ReplyTo = ActorRef[StatusReply[Done]]

  case class ProofCommand(command: Command, meta: WW, replyTo: ReplyTo)

  type Effect = akka.persistence.typed.scaladsl.Effect[Event, State]

  type ProofReplyEffect =
    akka.persistence.typed.scaladsl.ReplyEffect[ProofEvent, State]

  val EntityKey: EntityTypeKey[ProofCommand] = EntityTypeKey("Proof")

  def init(system: ActorSystem[_]): Unit =
    val behaviorFactory: EntityContext[ProofCommand] => Behavior[ProofCommand] =
      entityContext => ProofBehavior(entityContext.entityId)
    ClusterSharding(system).init(Entity(EntityKey)(behaviorFactory))

  def apply(persistenceId: String): Behavior[ProofCommand] =
    import ProofEventHandler.*
    EventSourcedBehavior
      .withEnforcedReplies[ProofCommand, ProofEvent, State](
        persistenceId = PersistenceId.ofUniqueId(persistenceId),
        emptyState = None,
        commandHandler = handleProofCommand,
        eventHandler = (state, event) =>
          state.handleEvent(event).orElse(unhandledEvent(event, state))
      )
      .withTagger(_ => Set(ProofEvent.Tag))

  def handleProofCommand(
      state: State,
      command: ProofCommand
  ): ProofReplyEffect =
    handleCommand(state, command.command) match
      case Some(events) => persist(events, command.meta, command.replyTo)
      case _            => unhandled(command.command, command.replyTo)

  def handleCommand(state: State, cmd: Command): Option[Seq[Event]] =
    state match
      case None =>
        cmd match
          case CreateProof(
                id,
                person,
                parameterId,
                criterionId,
                documents,
                Authorized(note)
              ) =>
            Some(
              Seq(
                ProofCreated(id, person, parameterId, criterionId, documents),
                ProofAuthorized(id, note)
              )
            )
          case CreateProof(
                id,
                person,
                parameterId,
                criterionId,
                documents,
                _
              ) =>
            Some(
              Seq(ProofCreated(id, person, parameterId, criterionId, documents))
            )
          case _ => None
      case Some(proof) if proof.isRevoked => None
      case Some(proof) if proof.isAuthorized =>
        cmd match
          case AuthorizeProof(id, note) => Some(Seq(ProofAuthorized(id, note)))
          case RevokeProof(id, reason, since, documents) =>
            Some(Seq((ProofRevoked(id, reason, since, documents))))
          case _ => None
      case Some(proof) =>
        cmd match
          case AuthorizeProof(id, note) => Some(Seq(ProofAuthorized(id, note)))
          case UpdateProof(id, documents) =>
            Some(Seq(ProofUpdated(id, documents)))
          case RevokeProof(id, reason, since, documents) =>
            Some(Seq((ProofRevoked(id, reason, since, documents))))
          case _ => None

  private def persist(
      events: Seq[Event],
      meta: WW,
      replyTo: ReplyTo
  ): ProofReplyEffect =
    Effect
      .persist(events.map(ProofEvent(_, meta)))
      .thenReply(replyTo)(_ => StatusReply.Ack)

  private def unhandled(command: Command, replyTo: ReplyTo): ProofReplyEffect =
    Effect.unhandled.thenReply(replyTo)(s =>
      StatusReply.error(CommandNotAvailable(command, s))
    )

  private def unhandledEvent(event: ProofEvent, state: State): Nothing =
    throw UnhandledEvent(event, state)
