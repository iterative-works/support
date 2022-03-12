import mill._, scalalib._, scalajslib._

import scalajslib.api.ModuleKind

import $file.fiftyforms.{build => ff}, ff.support._
import $file.fiftyforms.{domain => dmn}, dmn.DomainModule

object core extends PureCrossSbtModule

object codecs extends PureCrossSbtModule {
  def ivyDeps = Agg(Deps.zioJson)
  def moduleDeps = Seq(core, ff.tapir)
}

object endpoints extends PureCrossSbtModule {
  def moduleDeps = Seq(core, codecs, ff.tapir)
}

object domain extends Module {
  trait MdrDomainModule extends DomainModule {
    override def modelModules = Seq(core)
    override def codecsModules = Seq(codecs)
    override def endpointsModules = Seq(endpoints)
    override def repoModules = Seq(ff.mongo)
  }
  object proof extends MdrDomainModule
  object parameters extends MdrDomainModule
  object users extends MdrDomainModule
}

object app extends CommonJSModule with SbtModule {
  def ivyDeps = Agg(
    Deps.zio,
    Deps.laminar,
    Deps.zioJson,
    Deps.waypoint,
    Deps.urlDsl,
    Deps.laminextCore,
    Deps.laminextUI,
    Deps.laminextTailwind,
    Deps.laminextValidationCore,
    Deps.tapirSttpClient,
    Deps.sttpClientCore
  )

  def moduleDeps = Seq(
    core.js,
    ff.ui,
    domain.parameters.query.client,
    domain.parameters.command.client,
    domain.users.query.client,
    domain.users.command.client,
    domain.proof.query.client,
    domain.proof.command.client,
    endpoints.js
  )

  def moduleKind = ModuleKind.ESModule
}

object server extends CommonModule with SbtModule {
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
