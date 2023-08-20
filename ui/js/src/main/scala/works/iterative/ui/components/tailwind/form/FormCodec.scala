package works.iterative
package ui.components.tailwind.form

import zio.prelude.Validation

import scala.scalajs.js

import core.PlainMultiLine
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import scala.util.Try

trait FormCodec[V, A]:
  def toForm(v: V): A
  def toValue(r: A): Validated[V]

object FormCodec:
  given FormCodec[String, String] with
    def toForm(v: String): String = v
    def toValue(r: String): Validated[String] = Validation.fromPredicateWith(
      InvalidValue("error.empty.string")
    )(r)(t => Option(t).map(_.trim).exists(_.nonEmpty))

  given FormCodec[Option[String], String] with
    def toForm(v: Option[String]): String = v.getOrElse("")
    def toValue(r: String): Validated[Option[String]] = Validation.succeed(
      Option(r).map(_.trim).filter(_.nonEmpty)
    )

  given FormCodec[PlainMultiLine, String] with
    override def toForm(v: PlainMultiLine): String = v.toString
    override def toValue(r: String): Validated[PlainMultiLine] =
      PlainMultiLine(r).mapError(e => InvalidValue(e))

  given plainMultiLineCodec: FormCodec[Option[PlainMultiLine], String] with
    override def toForm(v: Option[PlainMultiLine]): String = v match
      case Some(t) => t.toString
      case _       => ""
    override def toValue(r: String): Validated[Option[PlainMultiLine]] =
      PlainMultiLine.opt(r).mapError(e => InvalidValue(e))

  given optionLocalDateCodec: FormCodec[Option[LocalDate], String] with
    val df = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    override def toForm(v: Option[LocalDate]): String =
      v.map(df.format(_)).getOrElse("")
    override def toValue(r: String): Validated[Option[LocalDate]] =
      Validation.succeed(Try(LocalDate.parse(r, df)).toOption)

  given optionBooleanCodec: FormCodec[Option[Boolean], Boolean] with
    override def toForm(v: Option[Boolean]): Boolean = v.getOrElse(false)
    override def toValue(r: Boolean): Validated[Option[Boolean]] =
      Validation.succeed(Some(r))

trait LowPriorityFormCodecs:
  given identityCodec[A]: FormCodec[A, A] with
    override def toForm(v: A): A = v
    override def toValue(r: A): Validated[A] = Validation.succeed(r)
