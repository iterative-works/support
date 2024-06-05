package works.iterative
package akka
package service.impl

import zio.*
import _root_.akka.actor.typed.ActorSystem
import _root_.akka.cluster.sharding.typed.scaladsl.ClusterSharding
import _root_.akka.cluster.sharding.typed.scaladsl.EntityRef
import _root_.akka.util.Timeout
import works.iterative.core.service.{IdGenerator, SequenceIdGeneratorFactory}
import java.time.Year

class DocumentCounterSequenceIdGeneratorFactory(
    system: ActorSystem[?],
    config: AkkaConfig
) extends SequenceIdGeneratorFactory:
    private val sharding = ClusterSharding(system)

    override def generatorFor(row: String): IdGenerator[Int] =
        val ref = sharding.entityRefFor(DocumentCounterBehavior.EntityKey, row)
        new DocumentCounterSequenceIdGenerator(ref, config)
end DocumentCounterSequenceIdGeneratorFactory

object DocumentCounterSequenceIdGeneratorFactory:
    val layer: RLayer[AkkaActorSystem, SequenceIdGeneratorFactory] =
        ZLayer {
            for
                system <- ZIO.serviceWith[AkkaActorSystem](_.system)
                config <- ZIO.config(AkkaConfig.config)
                _ <- ZIO.attempt(DocumentCounterBehavior.init(using system))
            yield DocumentCounterSequenceIdGeneratorFactory(system, config)
        }.orDie
end DocumentCounterSequenceIdGeneratorFactory

class DocumentCounterSequenceIdGenerator(
    ref: EntityRef[DocumentCounterBehavior.Command],
    config: AkkaConfig
) extends IdGenerator[Int]:
    given Timeout = config.timeout
    override def nextId: UIO[Int] =
        ZIO.fromFuture(_ => ref.ask[Int](DocumentCounterBehavior.Next(_))).orDie
end DocumentCounterSequenceIdGenerator

class YearlyDocumentCounterGenerator(
    row: String,
    factory: SequenceIdGeneratorFactory
) extends IdGenerator[(Int, Year)]:
    override def nextId: UIO[(Int, Year)] =
        for
            year <- Clock.currentDateTime.map(_.getYear())
            id <- factory.generatorFor(s"$row-${year}").nextId
        yield (id, Year.of(year))
end YearlyDocumentCounterGenerator

object YearlyDocumentCounterGenerator:
    def layer(
        row: String
    ): URLayer[SequenceIdGeneratorFactory, IdGenerator[(Int, Year)]] =
        ZLayer {
            for factory <- ZIO.service[SequenceIdGeneratorFactory]
            yield YearlyDocumentCounterGenerator(row, factory)
        }
end YearlyDocumentCounterGenerator
