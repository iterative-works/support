package mdr.pdb.server

import zio.*
import zio.config.ReadError
import zio.logging.*
import zio.logging.backend.SLF4J
import mdr.pdb.users.query.repo.*
import mdr.pdb.proof.query.repo.*
import mdr.pdb.proof.command.entity.*
import works.iterative.mongo.*
import org.mongodb.scala.MongoClient
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.typed.Cluster
import akka.cluster.typed.Join
import akka.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import akka.projection.ProjectionBehavior
import mdr.pdb.proof.query.projection.ProofProjection

object Main extends ZIOAppDefault:

  override def hook = SLF4J.slf4j(LogLevel.Debug)

  val runtimeLayer: URLayer[AppEnv, Runtime[AppEnv]] =
    ZLayer.fromZIO(ZIO.runtime[AppEnv])

  val mongoClientLayer: RLayer[ZEnv, MongoClient] =
    MongoConfig.fromEnv >>> MongoClient.layer

  val proofRepositoryLayer: RLayer[ZEnv, ProofRepository] =
    (mongoClientLayer >+> MongoProofConfig.fromEnv) >>> MongoProofRepository.layer

  val securityLayer: ZLayer[AppEnv, ReadError[String], HttpSecurity] =
    security.Pac4jSecurityConfig.fromEnv ++ runtimeLayer >>> security.Pac4jHttpSecurity.layer

  val actorSystemLayer: TaskLayer[ActorSystem[_]] =
    (for
      system <- Task.attempt(ActorSystem(Behaviors.empty, "mdrpdb"))
      _ <- Task.attempt {
        val cluster = Cluster(system)
        cluster.manager ! Join(cluster.selfMember.address)
      }
    yield system).toLayer

  val proofCommandBusLayer: RLayer[ZEnv, ProofCommandBus] =
    actorSystemLayer >>> ProofCommandBus.layer

  val appEnvLayer: RLayer[ZEnv, CustomAppEnv] =
    MockUsersRepository.layer >+> proofRepositoryLayer >+> proofCommandBusLayer

  val httpAppLayer: ZLayer[ZEnv, ReadError[String], HttpApplication] =
    AppConfig.fromEnv >>> HttpApplicationLive.layer

  val serverLayer: RLayer[ZEnv, HttpServer] =
    blaze.BlazeServerConfig.fromEnv >+> httpAppLayer >>> blaze.BlazeHttpServer.layer

  override def run =
    for {
      server <- ZIO
        .service[HttpServer]
        .provideCustom(serverLayer)
      _ <- (for
        system <- ZIO.service[ActorSystem[?]]
        _ <- ZIO.serviceWithZIO[ProofProjection] { pp =>
          Task.attempt {
            ShardedDaemonProcess(system).init[ProjectionBehavior.Command](
              name = "proof",
              numberOfInstances = 1,
              behaviorFactory = _ => ProjectionBehavior(pp.projection),
              stopMessage = ProjectionBehavior.Stop
            )
          }
        }
        _ <- server.serve()
      yield ()).provideCustom(appEnvLayer >+> securityLayer)
    } yield ()
