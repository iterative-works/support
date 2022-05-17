package works.iterative
package ui.components.tailwind.form

import zio.prelude.Validation

import scala.scalajs.js

import core.PlainMultiLine
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import scala.util.Try

trait FormCodec[V]:
  def toForm(v: V): String
  def toValue(r: String): Validated[V]

object FormCodec:
  given FormCodec[PlainMultiLine] with
    override def toForm(v: PlainMultiLine): String = v.toString
    override def toValue(r: String): Validated[PlainMultiLine] =
      PlainMultiLine(r).mapError(e => InvalidValue(e))

  given plainMultiLineCodec: FormCodec[Option[PlainMultiLine]] with
    override def toForm(v: Option[PlainMultiLine]): String = v match
      case Some(t) => t.toString
      case _       => ""
    override def toValue(r: String): Validated[Option[PlainMultiLine]] =
      PlainMultiLine.opt(r).mapError(e => InvalidValue(e))

  given optionLocalDateCodec: FormCodec[Option[LocalDate]] with
    val df = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    override def toForm(v: Option[LocalDate]): String =
      v.map(df.format(_)).getOrElse("")
    override def toValue(r: String): Validated[Option[LocalDate]] =
      Validation.succeed(Try(LocalDate.parse(r, df)).toOption)
