import sbt._
import Keys._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import org.scalajs.sbtplugin.ScalaJSPlugin
import scala.xml.XML
import scala.xml.Elem

object MockDataExport extends AutoPlugin {
  override lazy val requires = ScalaJSPlugin
  override lazy val trigger = noTrigger

  object autoImport {
    lazy val generateOrgDbData =
      taskKey[Seq[File]]("Generate org db data to JSON file")
    lazy val orgDbExportDir = settingKey[File]("Org db JSON export directory")
    lazy val orgDbOutputFile =
      settingKey[File]("Output file for the org DB JSON export")
    lazy val orgDbHeliosExportFile = settingKey[File]("HeliosData export file")
    lazy val orgDbDataExportFile = settingKey[File]("DataExport export file")
  }

  import autoImport._

  override def projectSettings = Seq(
    orgDbOutputFile := (Compile / target).value / "mock_data" / "users.json",
    orgDbExportDir := (Compile / sourceDirectory).value / "data",
    orgDbHeliosExportFile := orgDbExportDir.value / "HeliosData.xml",
    orgDbDataExportFile := orgDbExportDir.value / "DataExport_zam.xml",
    generateOrgDbData := {
      val file = orgDbOutputFile.value
      val heliosFile = orgDbHeliosExportFile.value
      def doExport() = {
        val heliosData =
          XML.loadFile(orgDbHeliosExportFile.value.getAbsolutePath())
        val exportData =
          XML.loadFile(orgDbDataExportFile.value.getAbsolutePath())
        IO.write(
          file,
          userData(heliosData, exportData)
        )
      }
      val cachedFun =
        FileFunction.cached(streams.value.cacheDirectory / "orgdb_export") {
          _ =>
            doExport()
            Set(file)
        }
      cachedFun(Set(heliosFile)).toSeq
    },
    (Compile / fastLinkJS) := (Compile / fastLinkJS)
      .dependsOn(generateOrgDbData)
      .value
  )

  def escaped(v: String): String = v.replaceAll("\"", "\\\"")
  def quoted(v: String): String = s""""${escaped(v)}""""

  def renderValue(v: Option[String]): Option[String] =
    v.map(i => s"${quoted(i)}")

  def renderParam(n: String, v: Option[String]): Option[String] =
    renderValue(v).map(r => s"""${quoted(n)}: ${r}""")

  def renderParams(params: List[(String, Option[String])]): String =
    params.map((renderParam _).tupled).flatten.mkString(", ")

  def renderUserTuple(
      id: String,
      params: List[(String, Option[String])],
      fce: Option[List[(String, Option[String])]],
      pp: List[List[(String, Option[String])]]
  ): String = {
    val contracts = pp.map(p => s"{${renderParams(p)}").mkString(", ")
    val mainF = fce
      .map(renderParams)
      .map(p => s""", "mainFunction": {$p}""")
      .getOrElse("")
    s""""$id": {${renderParams(
      params
    )} $mainF, "userContracts": [$contracts]}"""
  }

  def userData(heliosData: Elem, exportData: Elem): String = {
    val zamestnanci = (heliosData \\ "ExportZamestnanecHelios")
    val zamestnanciExport = (exportData \\ "ExportZamestnanec")

    def str(a: String) = (z: scala.xml.Node) => Some((z \ a).text.trim)
    def optstr(a: String) = (z: scala.xml.Node) =>
      Option((z \ a).text.trim).filterNot(_.isBlank)
    def mainFunction(a: String) = (z: scala.xml.Node) =>
      (z \\ "ExportFunkce")
        .find(n => (n \ "hlavniFunkce").text == "true")
        .flatMap(str(a))

    def mainContract(a: String) = (z: scala.xml.Node) =>
      (z \\ "ExportPracovniPomer")
        .find(n => (n \ "datumUkonceni").text.isEmpty)
        .flatMap(str(a))

    def id = str("osobniCislo").andThen(_.get)

    def parseRecord(attrs: List[(String, scala.xml.Node => Option[String])])(
        record: scala.xml.Node
    ): (String, List[(String, Option[String])]) =
      (id(record) -> attrs.map { case (n, g) => n -> g(record) })

    def parseHeliosUser(
        record: scala.xml.Node
    ): (String, List[(String, Option[String])]) =
      parseRecord(
        List(
          "personalNumber" -> str("osobniCislo"),
          "username" -> str("uzivatelskeJmeno"),
          "givenName" -> str("jmeno"),
          "surname" -> str("prijmeni"),
          "titlesBeforeName" -> optstr("titulPred"),
          "titlesAfterName" -> optstr("titulZa"),
          "email" -> optstr("email")
        )
      )(record)

    def parseMainFunction(
        record: scala.xml.Node
    ): (String, List[(String, Option[String])]) =
      parseRecord(
        List(
          "name" -> mainFunction("nazev"),
          "dept" -> mainFunction("nazevVOJ"),
          "ou" -> mainFunction("nazevStrediska")
        )
      )(record)

    def parseContract(
        record: scala.xml.Node
    ): (String, List[(String, Option[String])]) =
      parseRecord(
        List(
          "rel" -> mainContract("druh"),
          "startDate" -> mainContract("datumNastupu"),
          "endDate" -> mainContract("datumUkonceni")
        )
      )(record)

    val heliosUsers = Map(zamestnanci.map(parseHeliosUser): _*)
    val mainContracts = Map(zamestnanci.map(parseContract): _*)
    val mainFunctions = Map(zamestnanciExport.map(parseMainFunction): _*)

    val result = heliosUsers
      .map { case (id, params) =>
        val fce = mainFunctions.get(id)
        val contr = mainContracts.get(id)
        renderUserTuple(id, params, fce, contr.toList)
      }
      .mkString(",\n\t")

    s"{\n\t${result}\n}"
  }
}
