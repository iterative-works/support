package works.iterative
package akka
package service
package impl

import _root_.akka.persistence.typed.PersistenceId
import _root_.akka.actor.typed.ActorRef
import _root_.akka.cluster.sharding.typed.scaladsl.ClusterSharding
import _root_.akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import _root_.akka.actor.typed.ActorSystem
import _root_.akka.actor.typed.Behavior
import _root_.akka.cluster.sharding.typed.scaladsl.Entity
import _root_.akka.cluster.sharding.typed.scaladsl.EntityContext
import _root_.akka.persistence.typed.scaladsl.EventSourcedBehavior
import _root_.akka.persistence.typed.scaladsl.Effect
import _root_.akka.persistence.typed.scaladsl.RetentionCriteria

object DocumentCounterBehavior:
    type State = DocumentCounterState
    type Event = DocumentCounterEvent

    sealed trait Command
    case class Next(replyTo: ActorRef[Int]) extends Command

    val EntityKey: EntityTypeKey[Command] =
        EntityTypeKey[Command]("DocumentCounter")

    def init(using system: ActorSystem[?]): Unit =
        val behaviorFactory: EntityContext[Command] => Behavior[Command] =
            entityContext => DocumentCounterBehavior(entityContext.entityId)
        val _ = ClusterSharding(system).init(Entity(EntityKey)(behaviorFactory))
    end init

    def apply(persistenceId: String): Behavior[Command] =
        import DocumentCounterEvent.*
        EventSourcedBehavior
            .withEnforcedReplies[Command, Event, State](
                persistenceId = PersistenceId("DocumentCounter", persistenceId),
                emptyState = DocumentCounterState(0),
                commandHandler = (_, command) =>
                    command match
                        case Next(replyTo) =>
                            Effect
                                .persist(Incremented)
                                .thenReply(replyTo)(s => s.counter)
                ,
                eventHandler = (state, event) =>
                    event match
                        case Incremented => DocumentCounterState(state.counter + 1)
            )
            .withRetention(RetentionCriteria.snapshotEvery(100, 2))
    end apply
end DocumentCounterBehavior
