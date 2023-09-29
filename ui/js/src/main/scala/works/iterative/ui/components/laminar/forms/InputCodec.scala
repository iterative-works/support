package works.iterative.ui.components.laminar.forms

import works.iterative.core.*
import zio.prelude.Validation
import works.iterative.core.UserMessage

trait InputCodec[A]:
  def encode(a: A): String
  def decode(s: String): Validated[A]
  def decodeOptional(label: String)(s: Option[String]): Validated[A] =
    Validation.fail(UserMessage("error.value.required", label))
  def required: Boolean = true

object InputCodec:
  def apply[A](
      encodeF: A => String,
      decodeF: String => Validated[A]
  ): InputCodec[A] =
    new InputCodec[A]:
      override def encode(a: A): String = encodeF(a)
      override def decode(s: String): Validated[A] = decodeF(s)

  def fromValidatedString[A](
      factory: ValidatedStringFactory[A]
  ): InputCodec[A] = new InputCodec:
    override def encode(a: A): String = factory.getter(a)
    override def decode(s: String): Validated[A] = factory(s)

  given withValidatedStringFactory[A](using
      factory: ValidatedStringFactory[A]
  ): InputCodec[A] = fromValidatedString[A](factory)

  given validatedStringToInputCodec[A]
      : Conversion[ValidatedStringFactory[A], InputCodec[A]] =
    fromValidatedString(_)

  given string: InputCodec[String] with
    override def encode(a: String): String = a
    override def decode(s: String): Validated[String] = Validation.succeed(s)

  given plainOneLine: InputCodec[PlainOneLine] with
    override def encode(a: PlainOneLine): String = a.asString
    override def decode(s: String): Validated[PlainOneLine] =
      PlainOneLine(s)

  given plainMultiLine: InputCodec[PlainMultiLine] with
    override def encode(a: PlainMultiLine): String = a.asString
    override def decode(s: String): Validated[PlainMultiLine] =
      PlainMultiLine(s)

  given email: InputCodec[Email] with
    def encode(a: Email): String = a.value
    def decode(s: String): Validated[Email] = Email(s)

  given optionalInputCodec[A](using codec: InputCodec[A]): InputCodec[Option[A]]
    with
    def encode(a: Option[A]): String = a.map(codec.encode).getOrElse("")
    def decode(s: String): Validated[Option[A]] =
      if s.isEmpty then Validation.succeed(None)
      else codec.decode(s).map(Some(_))
    override def decodeOptional(label: String)(
        s: Option[String]
    ): Validated[Option[A]] =
      Validation.succeed(None)
    override def required: Boolean = false
