import mill._, scalalib._

import $file.{build => ff}, ff.support._

trait DomainModule extends Module {
  // General extra model deps
  def modelModules: Seq[Module] = Seq.empty
  // General extra codecs deps
  def codecsModules: Seq[Module] = Seq.empty
  // General extra endpoints deps
  def endpointsModules: Seq[Module] = Seq.empty

  // Implementation deps for repo
  def repoModules: Seq[JavaModule] = Seq.empty

  object shared extends Module {
    object model extends PureCrossModule {
      def moduleDeps = modelModules
      def ivyDeps = Agg(Deps.zioPrelude)
    }
    object codecs extends PureCrossModule {
      def moduleDeps = Seq(model) ++ codecsModules
      def ivyDeps = Agg(Deps.zioJson)
    }
  }

  trait CommonProjects extends Module {
    object model extends PureCrossModule {
      def ivyDeps = Agg(Deps.zioPrelude)
      def moduleDeps = Seq(shared.model)
    }
    object codecs extends PureCrossModule {
      def moduleDeps =
        Seq(model, shared.model, shared.codecs, ff.tapir)
    }
    object endpoints extends PureCrossModule {
      def ivyDeps = Agg(Deps.tapirCore, Deps.tapirZIOJson)
      def moduleDeps = Seq(model, codecs) ++ endpointsModules
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
      def moduleDeps = Seq(model.js, codecs.js, client, ff.ui)
    }
  }

  object query extends CommonProjects {
    object api extends CommonModule {
      def ivyDeps = Agg(Deps.zio, Deps.tapirZIOHttp4sServer)
      def moduleDeps = Seq(repo, query.endpoints.jvm)
    }
    object repo extends CommonModule {
      def ivyDeps = Agg(Deps.zio)
      def moduleDeps = Seq(model.jvm, codecs.jvm) ++ repoModules
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
