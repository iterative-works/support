package works.iterative.akka

import zio.*
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.typed.Cluster
import akka.cluster.typed.Join

case class AkkaActorSystem(system: ActorSystem[?]):
  val joinSelf: Task[Unit] = ZIO.attempt {
    val cluster = Cluster(system)
    cluster.manager ! Join(cluster.selfMember.address)
  }

object AkkaActorSystem:
  def empty(name: String): TaskLayer[AkkaActorSystem] =
    ZLayer(
      for system <- ZIO.attempt(ActorSystem(Behaviors.empty, name))
      yield AkkaActorSystem(system)
    )
