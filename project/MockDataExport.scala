import sbt._
import Keys._
import scala.xml.XML
import scala.xml.Elem

object MockDataExport extends AutoPlugin {
  override def trigger = noTrigger

  object autoImport {
    lazy val generateOrgDbData =
      taskKey[Seq[File]]("Generate org db data to JSON file")
    lazy val orgDbExportDir = settingKey[File]("Org db JSON export directory")
    lazy val orgDbOutputFile =
      settingKey[File]("Output file for the org DB JSON export")
    lazy val orgDbHeliosExportFile = settingKey[File]("HeliosData export file")
  }

  import autoImport._

  override def projectSettings = Seq(
    orgDbOutputFile := (Compile / target).value / "mock_data" / "users.json",
    orgDbExportDir := (Compile / sourceDirectory).value / "data",
    orgDbHeliosExportFile := orgDbExportDir.value / "HeliosData.xml",
    generateOrgDbData := {
      val file = orgDbOutputFile.value
      val heliosData =
        XML.loadFile(orgDbHeliosExportFile.value.getAbsolutePath())
      IO.write(
        file,
        userData(heliosData)
      )
      Seq(file)
    }
    // TODO: cached run & auto run on fastLinkJS
  )

  def escaped(v: String): String = v.replaceAll("\"", "\\\"")
  def quoted(v: String): String = s""""${escaped(v)}""""

  def renderValue(v: AnyRef): Option[String] = v match {
    case o: Option[String] @unchecked => o.map(i => s"${quoted(i)}")
    case _                            => Some(quoted(v.toString))
  }

  def renderParam(n: String, v: AnyRef): Option[String] =
    renderValue(v).map(r => s"""${quoted(n)}: ${r}""")

  def renderParams(params: List[(String, AnyRef)]): String =
    params.map((renderParam _).tupled).flatten.mkString(", ")

  def renderUserTuple(id: String, params: List[(String, AnyRef)]): String =
    s""""$id": {${renderParams(params)}}"""

  def userData(heliosData: Elem): String = {
    val zamestnanci = (heliosData \\ "ExportZamestnanecHelios")

    def str(a: String) = (z: scala.xml.Node) => (z \ a).text.trim
    def optstr(a: String) = (z: scala.xml.Node) =>
      Option((z \ a).text.trim).filterNot(_.isBlank)

    def id = str("osobniCislo")

    val attrs = List(
      "personalNumber" -> str("osobniCislo"),
      "username" -> str("uzivatelskeJmeno"),
      "givenName" -> str("jmeno"),
      "surname" -> str("prijmeni"),
      "titlesBeforeName" -> optstr("titulPred"),
      "titlesAfterName" -> optstr("titulZa"),
      "email" -> optstr("email")
    )

    def renderUser(record: scala.xml.Node): String =
      renderUserTuple(id(record), attrs.map { case (n, g) => n -> g(record) })

    def renderUserList(records: Seq[scala.xml.Node]): String =
      s"{\n\t${records.map(renderUser).mkString(",\n\t")}\n}"

    renderUserList(zamestnanci)
  }
}
