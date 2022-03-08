package mdr.pdb
package proof
package command
package entity

import akka.persistence.typed.scaladsl.EventSourcedBehavior
import akka.persistence.typed.PersistenceId
import akka.actor.typed.Behavior
import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import akka.Done

import fiftyforms.akka.*
import akka.persistence.typed.scaladsl.Effect

object ProofBehaviour:

  type ReplyTo = ActorRef[StatusReply[Done]]

  type Effect = akka.persistence.typed.scaladsl.Effect[Event, State]

  case class ProofCommand(command: Command, meta: WW, replyTo: ReplyTo)
  case class ProofEvent(event: Event, meta: WW)

  type ProofReplyEffect =
    akka.persistence.typed.scaladsl.ReplyEffect[ProofEvent, State]

  def apply(persistenceId: PersistenceId): Behavior[ProofCommand] =
    EventSourcedBehavior
      .withEnforcedReplies[ProofCommand, ProofEvent, State](
        persistenceId = persistenceId,
        emptyState = None,
        commandHandler = handleProofCommand,
        eventHandler = handleProofEvent
      )

  def handleProofCommand(
      state: State,
      command: ProofCommand
  ): ProofReplyEffect =
    handleCommand(state, command.command) match
      case Some(events) => persist(events, command.meta, command.replyTo)
      case _            => unhandled(command.command, command.replyTo)

  type ProofHandler = WW ?=> PartialFunction[Event, Proof]
  type ProofModHandler = WW ?=> PartialFunction[Event, Proof => Proof]

  def handleProofEvent(state: State, event: ProofEvent): State =
    val ProofEvent(ev, ww) = event

    def handle(h: ProofHandler): State =
      Some(h(using ww).applyOrElse(ev, unhandledEvent(event, state)))

    def handleMod(p: Proof)(h: ProofModHandler): State =
      Some(h(using ww).applyOrElse(ev, unhandledEvent(event, state))(p))

    state match
      case None =>
        handle(handleCreateProof)
      case Some(proof) =>
        handleMod(proof) {
          handleAuthorizeProof orElse handleUpdateProof orElse handleRevokeProof
        }

  def handleCreateProof: ProofHandler = {
    case ProofCreated(id, person, parameterId, criterionId, documents) =>
      Proof(
        id,
        person,
        parameterId,
        criterionId,
        documents,
        Nil,
        Nil,
        summon[WW]
      )
  }

  def handleAuthorizeProof: ProofModHandler = { case AuthorizeProof(id, note) =>
    proof =>
      proof.copy(authorizations =
        proof.authorizations :+ Authorization(summon[WW], note)
      )
  }

  def handleUpdateProof: ProofModHandler = { case UpdateProof(id, documents) =>
    proof => proof.copy(documents = documents)
  }

  def handleRevokeProof: ProofModHandler = {
    case RevokeProof(id, reason, since, documents) =>
      proof =>
        proof.copy(revocations =
          proof.revocations :+ Revocation(summon[WW], since, reason, documents)
        )
  }

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
