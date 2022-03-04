package mdr.pdb

import java.time.LocalDate
import zio.json.JsonCodec
import zio.json.DeriveJsonCodec
import zio.json.JsonFieldEncoder
import zio.json.JsonFieldDecoder

opaque type OsobniCislo = String

object OsobniCislo:
  // TODO: validation
  def apply(osc: String): OsobniCislo = osc

  extension (osc: OsobniCislo) def toString: String = osc

  given JsonCodec[OsobniCislo] =
    JsonCodec.string.transform(OsobniCislo.apply, _.toString)
  given JsonFieldEncoder[OsobniCislo] =
    JsonFieldEncoder.string.contramap(OsobniCislo.apply)
  given JsonFieldDecoder[OsobniCislo] = JsonFieldDecoder.string.map(_.toString)

case class UserContract(
    rel: String,
    startDate: LocalDate,
    endDate: Option[LocalDate]
)

object UserContract:
  given JsonCodec[UserContract] = DeriveJsonCodec.gen

case class UserFunction(name: String, dept: String, ou: String)

object UserFunction:
  given JsonCodec[UserFunction] = DeriveJsonCodec.gen

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

object UserInfo:
  given JsonCodec[UserInfo] = DeriveJsonCodec.gen

case class UserProfile(username: String, userInfo: UserInfo)

object UserProfile:
  given JsonCodec[UserProfile] = DeriveJsonCodec.gen
