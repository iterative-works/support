package cz.e_bs.cmi.mdr.pdb

opaque type OsobniCislo = String

object OsobniCislo:
  // TODO: validation
  def apply(osc: String): OsobniCislo = osc

extension (osc: OsobniCislo) def toString: String = osc

case class UserInfo(
    personalNumber: OsobniCislo,
    username: String,
    givenName: String,
    surname: String,
    titlesBeforeName: Option[String] = None,
    titlesAfterName: Option[String] = None,
    email: Option[String] = None,
    phone: Option[String] = None,
    organizationalUnit: Option[String] = None,
    mainFunction: Option[String] = None,
    img: Option[String] = None
) {
  val name =
    List(
      Some(
        List(titlesBeforeName, Some(givenName), Some(surname)).flatten.mkString(
          " "
        )
      ),
      titlesAfterName
    ).flatten.mkString(", ")
}

case class UserProfile(username: String, userInfo: UserInfo)
