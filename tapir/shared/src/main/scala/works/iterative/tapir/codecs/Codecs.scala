package works.iterative
package tapir.codecs

import zio.json.*
import zio.prelude.ZValidation
import works.iterative.tapir.CustomTapir.*
import works.iterative.core.*
import works.iterative.core.czech.*
import works.iterative.core.auth.*
import works.iterative.core.auth.service.AuthenticationError
import sttp.tapir.CodecFormat
import sttp.tapir.Validator
import works.iterative.core.service.FileStore

private[codecs] case class TextEncoding(
    pml: Option[PlainMultiLine],
    pon: Option[PlainOneLine],
    md: Option[Markdown]
)

case class LegacyFileRef(
    name: String,
    url: String,
    fileType: Option[String],
    size: Option[Long]
) derives JsonCodec

trait Codecs extends JsonCodecs with TapirCodecs

trait JsonCodecs:

  def fromValidation[T](
      v: ZValidation[?, UserMessage, T]
  ): Either[String, T] =
    v.mapError(_.id).toEither.left.map(_.mkString(","))

  def textCodec[T](
      f: String => ZValidation[?, UserMessage, T]
  ): JsonCodec[T] =
    JsonCodec.string.transformOrFail(f andThen fromValidation, _.toString)

  def validatedStringDecoder[A](
      factory: ValidatedStringFactory[A]
  ): JsonDecoder[A] =
    JsonDecoder.string.mapOrFail(factory.apply andThen fromValidation)

  def validatedStringEncoder[A](
      factory: ValidatedStringFactory[A]
  ): JsonEncoder[A] =
    JsonEncoder.string.contramap(factory.getter)

  def validatedStringCodec[A](
      factory: ValidatedStringFactory[A]
  ): JsonCodec[A] =
    JsonCodec(validatedStringEncoder(factory), validatedStringDecoder(factory))

  given fromValidatedStringCodec[A](using
      factory: ValidatedStringFactory[A]
  ): JsonCodec[A] = validatedStringCodec(factory)

  given JsonCodec[PlainMultiLine] = textCodec(PlainMultiLine.apply)
  given JsonCodec[PlainOneLine] = textCodec(PlainOneLine.apply)
  given JsonCodec[Markdown] = textCodec(Markdown.apply)
  given JsonCodec[HtmlText] = textCodec(HtmlText.apply)
  given JsonCodec[ICO] = validatedStringCodec(ICO)

  given JsonCodec[PermissionOp] =
    JsonCodec.string.transform(PermissionOp(_), _.value)
  given JsonCodec[PermissionTarget] = textCodec(PermissionTarget.apply)

  given JsonCodec[UserId] =
    JsonCodec.string.transform(UserId.unsafe(_), _.value)

  given JsonCodec[Email] = validatedStringCodec(Email)

  given JsonCodec[UserRole] = validatedStringCodec(UserRole)
  given JsonCodec[Avatar] = validatedStringCodec(Avatar)
  given JsonCodec[Claim] = DeriveJsonCodec.gen[Claim]
  given JsonCodec[BasicProfile] = DeriveJsonCodec.gen[BasicProfile]

  given fileRefEncoder: JsonEncoder[FileRef] = DeriveJsonEncoder.gen[FileRef]
  val fileRefDecoder: JsonDecoder[FileRef] = DeriveJsonDecoder.gen[FileRef]

  given completeFileRefDecoder: JsonDecoder[FileRef] =
    fileRefDecoder.orElse(
      JsonDecoder[LegacyFileRef].map(f =>
        FileRef(
          f.name,
          f.url,
          List(
            f.fileType.map(FileStore.Metadata.FileType -> _),
            f.size.map(FileStore.Metadata.Size -> _.toString())
          ).flatten.toMap
        )
      )
    )

  given JsonCodec[FileRef] = JsonCodec(fileRefEncoder, completeFileRefDecoder)

  given JsonCodec[Moment] = JsonCodec.instant.transform(
    Moment(_),
    _.toInstant
  )
  given JsonCodec[UserHandle] = DeriveJsonCodec.gen[UserHandle]
  given JsonCodec[AccessToken] =
    JsonCodec.string.transform(AccessToken(_), _.token)
  given JsonCodec[AuthedUserInfo] = DeriveJsonCodec.gen[AuthedUserInfo]
  given JsonCodec[AuthenticationError] =
    DeriveJsonCodec.gen[AuthenticationError]
  given JsonCodec[MessageId] =
    JsonCodec.string.transform(MessageId(_), _.toString())
  given JsonCodec[MessageArg] =
    JsonCodec.string.transform(
      input =>
        input match
          case i if i.startsWith("__int:")  => i.stripPrefix("__int:").toInt
          case i if i.startsWith("__long:") => i.stripPrefix("__long:").toLong
          case i if i.startsWith("__bool:") =>
            i.stripPrefix("__bool:").toBoolean
          case i if i.startsWith("__double:") =>
            i.stripPrefix("__double:").toDouble
          case i if i.startsWith("__char:") => i.stripPrefix("__char:").head
          case _                            => input
      ,
      arg =>
        arg match
          case i: Int     => s"__int:$i"
          case l: Long    => s"__long:$l"
          case b: Boolean => s"__bool:$b"
          case d: Double  => s"__double:$d"
          case c: Char    => s"__char:$c"
          case s: String  => s
    )
  given JsonCodec[UserMessage] = DeriveJsonCodec.gen[UserMessage]

trait TapirCodecs:
  given fromValidatedStringSchema[A](using
      ValidatedStringFactory[A]
  ): Schema[A] = Schema.string

  given fromValidatedStringTapirCodec[A](using
      factory: ValidatedStringFactory[A]
  ): Codec[String, A, CodecFormat.TextPlain] =
    Codec.string.mapDecode(v =>
      factory
        .apply(v)
        .fold(
          e =>
            DecodeResult
              .Error(v, new IllegalArgumentException(e.head.toString())),
          DecodeResult.Value(_)
        )
    )(_.value)

  given Codec[String, PermissionTarget, CodecFormat.TextPlain] =
    Codec.string.map(PermissionTarget.unsafe(_))(_.toString())

  given Schema[HtmlText] = Schema.string
  given Schema[PermissionOp] = Schema.string
  given Schema[PermissionTarget] = Schema.string
  given Schema[PlainMultiLine] = Schema.string
  given Schema[PlainOneLine] = Schema.string
  given Schema[Markdown] = Schema.string
  given Schema[UserId] = Schema.string
  given Schema[UserRole] = Schema.string
  given Schema[UserName] = Schema.string
  given Schema[Avatar] = Schema.string
  given Schema[Email] = Schema.string
  given Schema[Claim] = Schema.derived[Claim]
  given Schema[BasicProfile] = Schema.derived[BasicProfile]
  given Schema[FileRef] = Schema.derived[FileRef]
  given Schema[Moment] =
    Schema.schemaForInstant.map(i => Some(Moment(i)))(_.toInstant)
  given Schema[UserHandle] = Schema.derived[UserHandle]
  given Schema[AccessToken] = Schema.string
  given Schema[AuthedUserInfo] = Schema.derived[AuthedUserInfo]
  given Schema[AuthenticationError] = Schema.derived[AuthenticationError]
  given Schema[MessageId] = Schema.string
  given Schema[MessageArg] = Schema.string
  given Schema[UserMessage] = Schema.derived[UserMessage]

  given Codec[String, AccessToken, CodecFormat.TextPlain] =
    Codec.string.map(AccessToken(_))(_.token)

object Codecs extends Codecs
