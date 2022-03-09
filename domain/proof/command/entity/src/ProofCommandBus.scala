package mdr.pdb
package proof
package command
package entity

import zio.*
import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.EntityContext
import akka.actor.typed.Behavior
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.util.Timeout
import java.time.Instant

object ProofCommandBus:
  def submitCommand(command: Command): RIO[ProofCommandBus, Unit] =
    ZIO.serviceWithZIO(_.submitCommand(command))

  val layer: RLayer[ActorSystem[_], ProofCommandBus] =
    ZIO
      .serviceWithZIO[ActorSystem[_]](system =>
        for
          timeout <- Task.attempt(
            Timeout.create(
              system.settings.config.getDuration("proof-bus.timeout")
            )
          )
          // TODO: init only once
          _ <- Task.attempt(ProofBehavior.init(system))
        yield ProofCommandBus(system)(using timeout)
      )
      .toLayer

class ProofCommandBus(system: ActorSystem[_])(using timeout: Timeout):
  private val sharding = ClusterSharding(system)

  def submitCommand(command: Command): Task[Unit] =
    for
      entityRef <- Task.attempt(
        sharding.entityRefFor(ProofBehavior.EntityKey, command.id)
      )
      reply <- ZIO.fromFuture(_ =>
        entityRef.askWithStatus(
          ProofBehavior.ProofCommand(
            command,
            WhoWhen(OsobniCislo("0123"), Instant.now()),
            _
          )
        )
      )
    yield ()
