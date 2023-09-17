package works.iterative
package core
package codecs

import zio.json.*
import zio.prelude.Validation
import works.iterative.tapir.CustomTapir
import works.iterative.core.auth.*

private[codecs] case class TextEncoding(
    pml: Option[PlainMultiLine],
    pon: Option[PlainOneLine],
    md: Option[Markdown]
)

trait Codecs extends JsonCodecs with TapirCodecs

trait JsonCodecs:

  def fromValidation[T](v: Validation[UserMessage, T]): Either[String, T] =
    v.mapError(_.id).toEither.left.map(_.mkString(","))

  private def textCodec[T](
      f: String => Validation[UserMessage, T]
  ): JsonCodec[T] =
    JsonCodec.string.transformOrFail(f andThen fromValidation, _.toString)

  given JsonCodec[PlainMultiLine] = textCodec(PlainMultiLine.apply)
  given JsonCodec[PlainOneLine] = textCodec(PlainOneLine.apply)
  given JsonCodec[Markdown] = textCodec(Markdown.apply)

  given JsonCodec[UserId] =
    JsonCodec.string.transform(auth.UserId.unsafe(_), _.value)

  given JsonCodec[Email] = textCodec(Email.apply)

  given JsonCodec[UserName] = textCodec(UserName.apply)
  given JsonCodec[UserRole] = textCodec(UserRole.apply)
  given JsonCodec[Avatar] = textCodec(Avatar.apply)
  given JsonCodec[BasicProfile] = DeriveJsonCodec.gen[BasicProfile]

trait TapirCodecs extends CustomTapir:
  given Schema[PlainMultiLine] = Schema.string
  given Schema[PlainOneLine] = Schema.string
  given Schema[Markdown] = Schema.string
  given Schema[UserId] = Schema.string
  given Schema[UserRole] = Schema.string
  given Schema[UserName] = Schema.string
  given Schema[Avatar] = Schema.string
  given Schema[Email] = Schema.string
  given Schema[BasicProfile] = Schema.derived[BasicProfile]

object Codecs extends Codecs
