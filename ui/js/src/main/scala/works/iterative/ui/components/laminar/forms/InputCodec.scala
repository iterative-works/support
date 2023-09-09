package works.iterative.ui.components.laminar.forms

import works.iterative.core.{Email, PlainMultiLine, Validated}
import zio.prelude.Validation

trait InputCodec[A]:
  def encode(a: A): String
  def decode(s: String): Validated[A]

object InputCodec:
  given InputCodec[String] with
    override def encode(a: String): String = a
    override def decode(s: String): Validated[String] = Validation.succeed(s)

  given InputCodec[PlainMultiLine] with
    override def encode(a: PlainMultiLine): String = a.asString
    override def decode(s: String): Validated[PlainMultiLine] =
      PlainMultiLine(s)

  given InputCodec[Email] with
    def encode(a: Email): String = a.value
    def decode(s: String): Validated[Email] = Email(s)

  given optionalInputCodec[A](using codec: InputCodec[A]): InputCodec[Option[A]]
    with
    def encode(a: Option[A]): String = a.map(codec.encode).getOrElse("")
    def decode(s: String): Validated[Option[A]] =
      if s.isEmpty then Validation.succeed(None)
      else codec.decode(s).map(Some(_))
