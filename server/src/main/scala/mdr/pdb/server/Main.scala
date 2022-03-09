package mdr.pdb.server

import zio.*
import zio.config.ReadError
import zio.logging.*
import zio.logging.backend.SLF4J
import mdr.pdb.users.query.repo.*
import mdr.pdb.proof.query.repo.*
import mdr.pdb.proof.command.entity.*
import fiftyforms.mongo.*
import org.mongodb.scala.MongoClient
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors

object Main extends ZIOAppDefault:

  override def hook = SLF4J.slf4j(LogLevel.Debug)

  lazy val runtimeLayer: URLayer[AppEnv, Runtime[AppEnv]] =
    ZLayer.fromZIO(ZIO.runtime[AppEnv])

  lazy val mongoClientLayer: RLayer[ZEnv, MongoClient] =
    MongoConfig.fromEnv >>> MongoClient.layer

  lazy val proofRepositoryLayer: RLayer[ZEnv, ProofRepository] =
    (mongoClientLayer >+> MongoProofConfig.fromEnv) >>> MongoProofRepository.layer

  lazy val securityLayer: ZLayer[AppEnv, ReadError[String], HttpSecurity] =
    security.Pac4jSecurityConfig.fromEnv ++ runtimeLayer >>> security.Pac4jHttpSecurity.layer

  lazy val httpAppLayer: ZLayer[AppEnv, ReadError[String], HttpApplication] =
    AppConfig.fromEnv ++ securityLayer >>> HttpApplicationLive.layer

  lazy val actorSystemLayer: TaskLayer[ActorSystem[_]] =
    Task.attempt(ActorSystem(Behaviors.empty, "MDR PDB")).toLayer

  lazy val proofCommandBusLayer: RLayer[ZEnv, ProofCommandBus] =
    actorSystemLayer >>> ProofCommandBus.layer

  lazy val appEnvLayer
      : RLayer[ZEnv, UsersRepository & ProofRepository & ProofCommandBus] =
    MockUsersRepository.layer >+> proofRepositoryLayer >+> proofCommandBusLayer

  lazy val serverLayer: RLayer[ZEnv, HttpServer] =
    appEnvLayer >+> blaze.BlazeServerConfig.fromEnv >+> httpAppLayer >>> blaze.BlazeHttpServer.layer

  override def run =
    for {
      server <- ZIO
        .service[HttpServer]
        .provideCustom(serverLayer)
      _ <- server.serve().provideCustom(appEnvLayer)
    } yield ()
