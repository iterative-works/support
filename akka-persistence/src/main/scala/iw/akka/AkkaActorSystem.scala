package works.iterative.akka

import zio.*
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.typed.Cluster
import akka.cluster.typed.Join
import akka.NotUsed

case class AkkaActorSystem(system: ActorSystem[?]):
  val joinSelf: Task[Unit] = ZIO.attempt {
    val cluster = Cluster(system)
    cluster.manager ! Join(cluster.selfMember.address)
  }

  given ActorSystem[?] = system

object AkkaActorSystem:
  def empty(name: String): ZLayer[Scope, Throwable, AkkaActorSystem] =
    ZLayer(
      ZIO
        .acquireRelease(
          ZIO.attempt(ActorSystem(Behaviors.empty[NotUsed], name))
        )(system => ZIO.attempt(system.terminate()).orDie)
        .map(
          AkkaActorSystem(_)
        )
    )
