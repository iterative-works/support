package portaly.forms.service.impl.rest

import works.iterative.core.auth.UserId
import works.iterative.core.UserName
import zio.json.JsonCodec
import sttp.tapir.Schema

import works.iterative.tapir.codecs.Codecs.given
import works.iterative.core.Email

final case class CompanyDetails(
    nazev: Option[String],
    ulice: Option[String],
    mesto: Option[String],
    stat: Option[String],
    dic: Option[String],
    psc: Option[String]
) derives JsonCodec, Schema

final case class User(
    id: UserId,
    name: Option[UserName],
    ico: Option[String],
    email: Option[Email],
    givenName: Option[String],
    surname: Option[String],
    company: Option[CompanyDetails],
    dsId: Option[String]
) derives JsonCodec, Schema:
    val displayName: String = name.map(_.value).getOrElse(id.value)
end User
