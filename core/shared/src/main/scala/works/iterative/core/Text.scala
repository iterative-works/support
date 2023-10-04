package works.iterative
package core

import zio.prelude.Validation

/* UserText represents different, more specific variants of String to be able to
 * better identify what kind of String are we looking It is meant to be used
 * primarily for UI, but sometimes it is useful to store the different text
 * representations variantly, eg. it is useful to have the distinction even on
 * the level of the data model, not just in the UI
 */
object Text:

  def nonEmpty(s: String): Option[String] = Option(s).filterNot(_.trim.isEmpty)

  def validateNonEmpty[T](text: T)(using
      ev: T =:= String
  ): Validated[T] =
    Validation.fromPredicateWith[UserMessage, T](
      UserMessage("validation.text.empty")
    )(text)(t => ev(t).nonEmpty)

  def firstNewLine(t: String): Int =
    val lf = t.indexOf('\n')
    def cr = t.indexOf('\r')
    if lf != -1 then lf else cr

  def hasNewLine(t: String): Boolean = firstNewLine(t) != -1

  def firstLineOf(t: String): String =
    firstNewLine(t) match
      case -1 => t
      case i  => t.take(i)

opaque type PlainMultiLine = String

object PlainMultiLine:
  def apply(text: String): Validated[PlainMultiLine] =
    Text.validateNonEmpty(text)

  def unsafe(text: String): PlainMultiLine = text

  def opt(text: String): Validated[Option[PlainMultiLine]] =
    Validation.succeed(optDirect(text))

  def optDirect(text: String): Option[PlainMultiLine] =
    Text.nonEmpty(text)

  given string2plainMultiline: Conversion[String, Option[PlainMultiLine]] with
    def apply(text: String): Option[PlainMultiLine] = optDirect(text)

  given optString2PlainMultiline
      : Conversion[Option[String], Option[PlainMultiLine]] with
    def apply(text: Option[String]): Option[PlainMultiLine] =
      text.flatMap(optDirect)

  given plainMultiLine2String: Conversion[PlainMultiLine, String] with
    def apply(p: PlainMultiLine): String = p.toString

  given optionPlainMultiLine2OptionString
      : Conversion[Option[PlainMultiLine], Option[String]] with
    def apply(p: Option[PlainMultiLine]): Option[String] = p.map(_.toString)

  extension (p: PlainMultiLine) def asString: String = p

opaque type PlainOneLine = String

object PlainOneLine:
  def validateOneLine(text: PlainOneLine): Validated[PlainOneLine] =
    Validation.fromPredicateWith[UserMessage, PlainOneLine](
      UserMessage("validation.text.oneline")
    )(text)(!Text.hasNewLine(_))

  def apply(text: String): Validated[PlainOneLine] =
    for
      _ <- Text.validateNonEmpty(text)
      _ <- validateOneLine(text)
    yield text

  def unsafe(text: String): PlainOneLine = text

  def opt(text: String): Validated[Option[PlainOneLine]] =
    for _ <- validateOneLine(text)
    yield Text.nonEmpty(text)

  def optFirstLine(text: String): Option[PlainOneLine] =
    Text.nonEmpty(Text.firstLineOf(text))

  def firstLine(text: String, default: => String): PlainOneLine =
    optFirstLine(text).getOrElse(default)

  def firstLineEmpty(text: String): PlainOneLine = firstLine(text, "")

  given string2FirstLineEmpty: Conversion[String, PlainOneLine] =
    firstLineEmpty(_)

  extension (p: PlainOneLine) def asString: String = p

opaque type Markdown = String

object Markdown:
  def apply(text: String): Validated[Markdown] =
    Text.validateNonEmpty(text)

  def opt(text: String): Validated[Option[Markdown]] =
    Validation.succeed(optDirect(text))

  def optDirect(text: String): Option[Markdown] =
    Text.nonEmpty(text)

  extension (p: Markdown) def asString: String = p
