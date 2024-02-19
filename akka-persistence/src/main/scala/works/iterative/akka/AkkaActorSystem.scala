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

  val terminate: UIO[Unit] = ZIO.attempt(system.terminate()).orDie

  given ActorSystem[?] = system

object AkkaActorSystem:
  private def makeEmpty(name: String): ZIO[Scope, Throwable, AkkaActorSystem] =
    ZIO.acquireRelease(
      ZIO
        .attempt(ActorSystem(Behaviors.empty[NotUsed], name))
        .map(AkkaActorSystem(_))
    )(_.terminate)

  def empty(name: String): ZLayer[Scope, Throwable, AkkaActorSystem] =
    ZLayer(makeEmpty(name))

  def emptySingleNodeCluster(
      name: String
  ): ZLayer[Scope, Throwable, AkkaActorSystem] =
    ZLayer(makeEmpty(name).tap(_.joinSelf))
