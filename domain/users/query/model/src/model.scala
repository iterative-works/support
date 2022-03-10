package mdr.pdb
package users.query

import java.time.LocalDate

sealed trait Criteria
case object AllUsers extends Criteria
case class UserWithOsobniCislo(osobniCislo: OsobniCislo) extends Criteria

case class UserContract(
    rel: String,
    startDate: LocalDate,
    endDate: Option[LocalDate]
)

case class UserFunction(name: String, dept: String, ou: String)

case class UserInfo(
    personalNumber: OsobniCislo,
    username: String,
    givenName: String,
    surname: String,
    titlesBeforeName: Option[String] = None,
    titlesAfterName: Option[String] = None,
    email: Option[String] = None,
    phone: Option[String] = None,
    mainFunction: Option[UserFunction] = None,
    userContracts: List[UserContract] = Nil,
    img: Option[String] = None
):
  val name =
    List(
      Some(
        List(titlesBeforeName, Some(givenName), Some(surname)).flatten.mkString(
          " "
        )
      ),
      titlesAfterName
    ).flatten.mkString(", ")

case class UserProfile(username: String, userInfo: UserInfo)
