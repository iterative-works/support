import sbt._
import Keys._

import sbtcrossproject.JVMPlatform
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
      json: CrossProject,
      query: QueryProjects,
      command: CommandProjects
  ) extends CompositeProject {
    def model(upd: CrossProject => CrossProject): EntityProject =
      copy(model = upd(model))
    def json(upd: CrossProject => CrossProject): EntityProject =
      copy(json = upd(json))
    def query(upd: QueryProjects => QueryProjects): EntityProject =
      copy(query = upd(query))
    def command(upd: CommandProjects => CommandProjects): EntityProject =
      copy(command = upd(command))
    def endpoints(upd: CrossProject => CrossProject): EntityProject =
      query(_.endpoints(upd)).command(_.endpoints(upd))
    def components(upd: Project => Project): EntityProject =
      query(_.components(upd)).command(_.components(upd))
    override def componentProjects: Seq[Project] =
      Seq(model, json).flatMap(
        _.componentProjects
      ) ++ query.componentProjects ++ command.componentProjects
  }

  case class CommonProjects(
      model: CrossProject,
      json: CrossProject,
      endpoints: CrossProject,
      client: Project,
      api: Project,
      components: Project
  ) extends CompositeProject {
    def model(upd: CrossProject => CrossProject): CommonProjects =
      copy(model = upd(model))
    def json(upd: CrossProject => CrossProject): CommonProjects =
      copy(json = upd(json))
    def endpoints(upd: CrossProject => CrossProject): CommonProjects =
      copy(endpoints = upd(endpoints))
    def client(upd: Project => Project): CommonProjects =
      copy(client = upd(client))
    def api(upd: Project => Project): CommonProjects = copy(api = upd(api))
    def components(upd: Project => Project): CommonProjects =
      copy(components = upd(components))
    override def componentProjects: Seq[Project] =
      Seq(model, json, endpoints).flatMap(
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
    val json = common.json
    def json(upd: CrossProject => CrossProject): QueryProjects =
      copy(common = common.json(upd))
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
    val json = common.json
    def json(upd: CrossProject => CrossProject): CommandProjects =
      copy(common = common.json(upd))
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

  object EntityProject {
    class ProjectBuilder(b: String, base: File)(kind: String) {
      def name(n: String) = s"$b-$kind-$n"
      def path(n: String) = base / kind / n
      def p(n: String): Project = Project(name(n), path(n))
      def cp(n: String): CrossProject =
        CrossProject(name(n), path(n))(JSPlatform, JVMPlatform)
          .crossType(CrossType.Pure)
      def js(n: String): Project = p(n).enablePlugins(ScalaJSPlugin)
    }

    def apply(b: String, base: File): EntityProject = {
      def pb(kind: String) = new ProjectBuilder(b, base)(kind)
      val sh = pb("shared")
      val sharedModel = sh.cp("model").settings(IWDeps.zioPrelude)
      val sharedJson =
        sh.cp("json").settings(IWDeps.zioJson).dependsOn(sharedModel)

      def commonProjects(kb: ProjectBuilder) = {
        import kb._
        val model: CrossProject = cp("model").dependsOn(sharedModel)
        val json: CrossProject =
          cp("json").dependsOn(model, sharedJson)
        val endpoints: CrossProject = cp("endpoints")
          .settings(
            IWDeps.tapirCore,
            IWDeps.tapirZIOJson
          )
          .dependsOn(model, json)
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
        CommonProjects(model, json, endpoints, client, api, components)
      }

      def queryProjects = {
        val qb = pb("query")
        import qb._
        val common = commonProjects(qb)
        val repo = p("repo")
          .settings(IWDeps.useZIO(Test))
          .dependsOn(
            common.model.projects(JVMPlatform),
            common.json.projects(JVMPlatform)
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
        CommandProjects(commonProjects(cb), p("entity"))
      }

      EntityProject(
        model = sharedModel,
        json = sharedJson,
        query = queryProjects,
        command = commandProjects
      )
    }
  }
}
