import sbt._
import Keys._

import sbtcrossproject.{JVMPlatform, Platform}
import sbtcrossproject.CrossPlugin.autoImport._
import sbtcrossproject.CrossProject
import scalajscrossproject.JSPlatform

import org.scalajs.sbtplugin.ScalaJSPlugin
import sbt.plugins.JvmPlugin

import works.iterative.sbt.IWMaterialsPlugin.autoImport._

object DomainProjectsPlugin extends AutoPlugin {
  override def trigger = allRequirements

  object autoImport {
    def entityProject(name: String, base: File): EntityProject =
      EntityProject(name, base)
  }

  case class EntityProject(
      model: CrossProject,
      codecs: CrossProject,
      query: QueryProjects,
      command: CommandProjects
  ) extends CompositeProject {
    def model(upd: CrossProject => CrossProject): EntityProject =
      copy(model = upd(model))
    def codecs(upd: CrossProject => CrossProject): EntityProject =
      copy(codecs = upd(codecs))
    def query(upd: QueryProjects => QueryProjects): EntityProject =
      copy(query = upd(query))
    def command(upd: CommandProjects => CommandProjects): EntityProject =
      copy(command = upd(command))
    def endpoints(upd: CrossProject => CrossProject): EntityProject =
      query(_.endpoints(upd)).command(_.endpoints(upd))
    def components(upd: Project => Project): EntityProject =
      query(_.components(upd)).command(_.components(upd))
    def repo(upd: Project => Project): EntityProject = query(_.repo(upd))
    def projection(upd: Project => Project): EntityProject = query(
      _.projection(upd)
    )
    def entity(upd: Project => Project): EntityProject = command(_.entity(upd))
    override def componentProjects: Seq[Project] =
      Seq(model, codecs).flatMap(
        _.componentProjects
      ) ++ query.componentProjects ++ command.componentProjects
  }

  case class CommonProjects(
      model: CrossProject,
      codecs: CrossProject,
      endpoints: CrossProject,
      client: Project,
      api: Project,
      components: Project
  ) extends CompositeProject {
    def model(upd: CrossProject => CrossProject): CommonProjects =
      copy(model = upd(model))
    def codecs(upd: CrossProject => CrossProject): CommonProjects =
      copy(codecs = upd(codecs))
    def endpoints(upd: CrossProject => CrossProject): CommonProjects =
      copy(endpoints = upd(endpoints))
    def client(upd: Project => Project): CommonProjects =
      copy(client = upd(client))
    def api(upd: Project => Project): CommonProjects = copy(api = upd(api))
    def components(upd: Project => Project): CommonProjects =
      copy(components = upd(components))
    override def componentProjects: Seq[Project] =
      Seq(model, codecs, endpoints).flatMap(
        _.componentProjects
      ) ++ Seq(client, api, components)
  }

  case class QueryProjects(
      common: CommonProjects,
      repo: Project,
      projection: Project
  ) extends CompositeProject {
    val model = common.model
    def model(upd: CrossProject => CrossProject): QueryProjects =
      copy(common = common.model(upd))
    val codecs = common.codecs
    def codecs(upd: CrossProject => CrossProject): QueryProjects =
      copy(common = common.codecs(upd))
    val endpoints = common.endpoints
    def endpoints(upd: CrossProject => CrossProject): QueryProjects =
      copy(common = common.endpoints(upd))
    val client = common.client
    def client(upd: Project => Project): QueryProjects =
      copy(common = common.client(upd))
    val api = common.api
    def api(upd: Project => Project): QueryProjects =
      copy(common = common.api(upd))
    val components = common.components
    def components(upd: Project => Project): QueryProjects =
      copy(common = common.components(upd))
    def repo(upd: Project => Project): QueryProjects = copy(repo = upd(repo))
    def projection(upd: Project => Project): QueryProjects =
      copy(projection = upd(projection))
    override def componentProjects: Seq[Project] =
      common.componentProjects ++ Seq(repo, projection)
  }

  case class CommandProjects(common: CommonProjects, entity: Project)
      extends CompositeProject {
    val model = common.model
    def model(upd: CrossProject => CrossProject): CommandProjects =
      copy(common = common.model(upd))
    val codecs = common.codecs
    def codecs(upd: CrossProject => CrossProject): CommandProjects =
      copy(common = common.codecs(upd))
    val endpoints = common.endpoints
    def endpoints(upd: CrossProject => CrossProject): CommandProjects =
      copy(common = common.endpoints(upd))
    val client = common.client
    def client(upd: Project => Project): CommandProjects =
      copy(common = common.client(upd))
    val api = common.api
    def api(upd: Project => Project): CommandProjects =
      copy(common = common.api(upd))
    val components = common.components
    def components(upd: Project => Project): CommandProjects =
      copy(common = common.components(upd))
    def entity(upd: Project => Project): CommandProjects =
      copy(entity = upd(entity))
    override def componentProjects: Seq[Project] =
      common.componentProjects :+ entity
  }

  object MiniCrossType extends CrossType {
    @deprecated(
      "use projectDir(crossBase: File, platform: Platform): File",
      "0.1.0"
    )
    def projectDir(crossBase: File, projectType: String): File =
      crossBase / ("." + projectType)

    def projectDir(crossBase: File, platform: Platform): File =
      crossBase / ("." + platform.identifier)

    def sharedSrcDir(projectBase: File, conf: String): Option[File] = {
      val dirName = conf match {
        case "main" => "src"
        case "test" => "test"
        case c @ _  => s"$c-src"
      }
      Some(projectBase.getParentFile / dirName)
    }

    override def sharedResourcesDir(
        projectBase: File,
        conf: String
    ): Option[File] = {
      val dirName = conf match {
        case "main" => "resources"
        case c @ _  => s"$c-resources"
      }
      Some(projectBase.getParentFile / dirName)
    }
  }

  object EntityProject {
    class ProjectBuilder(b: String, base: File)(kind: String) {
      val commonSettings = Seq(
        Compile / scalaSource := baseDirectory.value / "src",
        Test / scalaSource := baseDirectory.value / "test",
        Compile / resourceDirectory := baseDirectory.value / "resources",
        Test / resourceDirectory := baseDirectory.value / "test-resources"
      )
      def name(n: String) = s"$b-$kind-$n"
      def path(n: String) = base / kind / n
      def p(n: String): Project =
        Project(name(n), path(n)).settings(commonSettings)
      def cp(n: String): CrossProject =
        CrossProject(name(n), path(n))(JSPlatform, JVMPlatform)
          .crossType(MiniCrossType)
          .settings(commonSettings)
      def js(n: String): Project =
        p(n).enablePlugins(ScalaJSPlugin).settings(commonSettings)
    }

    def apply(b: String, base: File): EntityProject = {
      def pb(kind: String) = new ProjectBuilder(b, base)(kind)
      val sh = pb("shared")
      val sharedModel = sh.cp("model").settings(IWDeps.zioPrelude)
      val sharedCodecs =
        sh.cp("codecs").settings(IWDeps.zioJson).dependsOn(sharedModel)

      def commonProjects(kb: ProjectBuilder) = {
        import kb._
        val model: CrossProject = cp("model").dependsOn(sharedModel)
        val codecs: CrossProject =
          cp("codecs").dependsOn(model, sharedCodecs)
        val endpoints: CrossProject = cp("endpoints")
          .settings(
            IWDeps.tapirCore,
            IWDeps.tapirZIOJson
          )
          .dependsOn(model, codecs)
        val client: Project =
          js("client").dependsOn(endpoints.projects(JSPlatform))
        val api: Project = p("api")
          .settings(
            IWDeps.useZIO(),
            IWDeps.tapirZIOHttp4sServer
          )
          .dependsOn(endpoints.projects(JVMPlatform))
        val components: Project = js("components")
          .settings(
            IWDeps.laminar,
            IWDeps.laminextCore,
            IWDeps.laminextUI,
            IWDeps.laminextTailwind,
            IWDeps.laminextValidationCore
          )
        CommonProjects(model, codecs, endpoints, client, api, components)
      }

      def queryProjects = {
        val qb = pb("query")
        import qb._
        val common = commonProjects(qb)
        val repo = p("repo")
          .settings(IWDeps.useZIO(Test))
          .dependsOn(
            common.model.projects(JVMPlatform),
            common.codecs.projects(JVMPlatform)
          )
        QueryProjects(
          common.api(_.dependsOn(repo)),
          repo,
          p("projection").dependsOn(repo)
        )
      }

      def commandProjects = {
        val cb = pb("command")
        import cb._
        val common = commonProjects(cb)
        val entity =
          p("entity").dependsOn(
            common.model.projects(JVMPlatform),
            common.codecs.projects(JVMPlatform)
          )
        CommandProjects(
          common.api(_.dependsOn(entity)),
          entity
        )
      }

      EntityProject(
        model = sharedModel,
        codecs = sharedCodecs,
        query = queryProjects,
        command = commandProjects
      )
    }
  }
}
