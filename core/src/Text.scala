package works.iterative
package core

import scala.jdk.OptionConverters.given
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
  ): Validation[MessageId, T] =
    Validation.fromPredicateWith[MessageId, T](
      "validation.text.empty"
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
  def apply(text: String): Validation[MessageId, PlainMultiLine] =
    Text.validateNonEmpty(text)

  def opt(text: String): Validation[Nothing, Option[PlainMultiLine]] =
    Validation.succeed(optDirect(text))

  def optDirect(text: String): Option[PlainMultiLine] =
    Text.nonEmpty(text)

  extension (p: PlainMultiLine) def toString: String = p

opaque type PlainOneLine = String

object PlainOneLine:
  def validateOneLine(text: PlainOneLine): Validation[MessageId, PlainOneLine] =
    Validation.fromPredicateWith[MessageId, PlainOneLine](
      "validation.text.oneline"
    )(text)(!Text.hasNewLine(_))

  def apply(text: String): Validation[MessageId, PlainOneLine] =
    for
      _ <- Text.validateNonEmpty(text)
      _ <- validateOneLine(text)
    yield text

  def opt(text: String): Validation[MessageId, Option[PlainOneLine]] =
    for _ <- validateOneLine(text)
    yield Text.nonEmpty(text)

  def optFirstLine(text: String): Option[PlainOneLine] =
    Text.nonEmpty(Text.firstLineOf(text))

  def firstLine(text: String, default: => String): PlainOneLine =
    optFirstLine(text).getOrElse(default)

  def firstLineEmpty(text: String): PlainOneLine = firstLine(text, "")

  extension (p: PlainOneLine) def toString: String = p

opaque type Markdown = String

object Markdown:
  def apply(text: String): Validation[MessageId, Markdown] =
    Text.validateNonEmpty(text)

  def opt(text: String): Validation[Nothing, Option[Markdown]] =
    Validation.succeed(optDirect(text))

  def optDirect(text: String): Option[Markdown] =
    Text.nonEmpty(text)

  extension (p: Markdown) def toString: String = p
