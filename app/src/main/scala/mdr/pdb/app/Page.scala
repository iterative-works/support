package mdr.pdb.app

import mdr.pdb.OsobniCislo
import mdr.pdb.UserInfo
import mdr.pdb.Parameter
import mdr.pdb.ParameterCriteria

// enum is not working with Waypoints' SplitRender collectStatic
sealed abstract class Page(
    val id: String,
    val title: String,
    val parent: Option[Page]
) {
  val path: Vector[Page] =
    parent match
      case None    => Vector(this)
      case Some(p) => p.path :+ this

  val isRoot: Boolean = parent.isEmpty
}

object Page:

  val homePage: Page = Page.Directory

  case class Titled[V](value: V, title: Option[String] = None):
    val show: String = title.getOrElse(value.toString)

  case object Directory extends Page("directory", "Adresář", None)

  case object Dashboard extends Page("dashboard", "Přehled", Some(Directory))

  case class Detail(osobniCislo: Titled[OsobniCislo])
      extends Page("user", osobniCislo.show, Some(Directory))

  object Detail {
    def apply(o: UserInfo): Detail = Detail(
      Titled(o.personalNumber, Some(o.name))
    )
  }

  case class DetailParametru(
      osobniCislo: Titled[OsobniCislo],
      parametr: Titled[String]
  ) extends Page(
        "parameter",
        parametr.show,
        Some(Detail(osobniCislo))
      )

  object DetailParametru {
    def apply(o: UserInfo, p: Parameter): DetailParametru =
      DetailParametru(
        Titled(o.personalNumber, Some(o.name)),
        Titled(p.id, Some(p.name))
      )
  }

  case class DetailKriteria(
      osobniCislo: Titled[OsobniCislo],
      parametr: Titled[String],
      kriterium: Titled[String]
  ) extends Page(
        "criteria",
        kriterium.show,
        Some(DetailParametru(osobniCislo, parametr))
      )

  object DetailKriteria {
    def apply(o: UserInfo, p: Parameter, k: ParameterCriteria): DetailKriteria =
      DetailKriteria(
        Titled(o.personalNumber, Some(o.name)),
        Titled(p.id, Some(p.name)),
        Titled(k.id, Some(k.id))
      )
  }

  case class UpravDukazKriteria(
      osobniCislo: Titled[OsobniCislo],
      parametr: Titled[String],
      kriterium: Titled[String]
  ) extends Page(
        "addProof",
        "Důkaz",
        Some(DetailKriteria(osobniCislo, parametr, kriterium))
      )

  object UpravDukazKriteria {
    def apply(
        o: UserInfo,
        p: Parameter,
        k: ParameterCriteria
    ): UpravDukazKriteria =
      UpravDukazKriteria(
        Titled(o.personalNumber, Some(o.name)),
        Titled(p.id, Some(p.name)),
        Titled(k.id, Some(k.id))
      )
  }

  case class NotFound(url: String) extends Page("404", "404", Some(Directory))

  case class UnhandledError(
      errorName: Option[String],
      errorMessage: Option[String]
  ) extends Page("500", "Unexpected error", Some(Directory))
