import mill._, scalalib._, scalajslib._

import $file.fiftyforms.{build => ff}, ff.support._

object core extends PureCrossSbtModule

object codecs extends PureCrossSbtModule {
  def ivyDeps = Agg(Deps.zioJson)
  def moduleDeps = Seq(core, ff.tapir)
}

val coreCodecs = codecs

object endpoints extends PureCrossSbtModule {
  def moduleDeps = Seq(core, codecs, ff.tapir)
}

val coreEndpoints = endpoints

object domain extends Module {
  trait DomainModule extends Module {
    object shared extends Module {
      object model extends PureCrossModule {
        def moduleDeps = Seq(core)
        def ivyDeps = Agg(Deps.zioPrelude)
      }
      object codecs extends PureCrossModule {
        def moduleDeps = Seq(model, coreCodecs)
        def ivyDeps = Agg(Deps.zioJson)
      }
    }

    trait CommonProjects extends Module {
      object model extends PureCrossModule {
        def ivyDeps = Agg(Deps.zioPrelude)
        def moduleDeps = Seq(shared.model, core)
      }
      object codecs extends PureCrossModule {
        def moduleDeps =
          Seq(coreCodecs, model, shared.model, shared.codecs, ff.tapir)
      }
      object endpoints extends PureCrossModule {
        def ivyDeps = Agg(Deps.tapirCore, Deps.tapirZIOJson)
        def moduleDeps = Seq(model, codecs, coreEndpoints)
      }
      object client extends CommonJSModule {
        def moduleDeps = Seq(endpoints.js)
      }
      object components extends CommonJSModule {
        def ivyDeps = Agg(
          Deps.laminar,
          Deps.laminextCore,
          Deps.laminextUI,
          Deps.laminextTailwind,
          Deps.laminextValidationCore
        )
        def moduleDeps = Seq(ff.ui)
      }
    }

    object query extends CommonProjects {
      object api extends CommonModule {
        def ivyDeps = Agg(Deps.zio, Deps.tapirZIOHttp4sServer)
        def moduleDeps = Seq(repo, query.endpoints.jvm)
      }
      object repo extends CommonModule {
        def ivyDeps = Agg(Deps.zio)
        def moduleDeps = Seq(model.jvm, codecs.jvm, ff.mongo)
      }
      object projection extends CommonModule {
        def moduleDeps = Seq(repo, ff.akkaPersistence)
      }
    }

    object command extends CommonProjects {
      object api extends CommonModule {
        def ivyDeps = Agg(Deps.zio, Deps.tapirZIOHttp4sServer)
        def moduleDeps = Seq(entity, command.endpoints.jvm)
      }
      object entity extends CommonModule {
        def moduleDeps = Seq(model.jvm, codecs.jvm, ff.akkaPersistence)
      }
    }
  }

  object proof extends DomainModule
  object parameters extends DomainModule
  object users extends DomainModule
}

object server extends CommonModule {
  def moduleDeps = Seq(
    core.jvm,
    domain.parameters.query.api,
    domain.parameters.command.api,
    domain.users.query.api,
    domain.users.command.api,
    domain.proof.query.api,
    domain.proof.command.api,
    domain.proof.query.projection,
    domain.proof.command.entity,
    endpoints.jvm
  )
  def ivyDeps = Agg(
    Deps.zio,
    Deps.zioConfig,
    Deps.zioConfigTypesafe,
    Deps.zioConfigMagnolia,
    Deps.zioLoggingSlf4j,
    Deps.zioInteropCats,
    Deps.tapirCore,
    Deps.tapirZIO,
    Deps.tapirZIOJson,
    Deps.tapirZIOHttp4sServer,
    Deps.http4sBlazeServer,
    Deps.logbackClassic,
    Deps.http4sPac4J,
    Deps.pac4jOIDC,
    ivy"mysql:mysql-connector-java:8.0.28"
  )
}
