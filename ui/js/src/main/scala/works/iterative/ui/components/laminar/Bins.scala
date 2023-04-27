package works.iterative.ui.components.laminar

import com.raquo.laminar.api.L.{*, given}
import works.iterative.ui.components.tailwind.experimental.ColorDef
import com.raquo.laminar.builders.HtmlTag

case class Bin[Source, +Value](
    label: String,
    description: Option[String | HtmlElement],
    color: ColorDef,
    valueOf: Source => Value
):
  def map[NewValue](
      f: Bin[Source, Value] => Source => NewValue
  ): Bin[Source, NewValue] =
    copy(valueOf = f(this))

case class Bins[T, U](bins: Seq[Bin[T, U]]):
  def apply(v: T): Seq[U] = bins.map(_.valueOf(v))
  def map[A](r: Bin[T, U] => T => A): Bins[T, A] =
    copy(bins = bins.map(_.map(r)))

  def renderSeparated(
      r: Bin[T, U] => T => HtmlElement,
      separator: => HtmlElement = span(" / "),
      container: HtmlTag[org.scalajs.dom.html.Element] = span
  )(v: T): HtmlElement =
    container(interleave(map(r)(v), separator))

  def interleave(
      a: Seq[HtmlElement],
      separator: => HtmlElement
  ): Seq[HtmlElement] =
    if a.size < 2 then a
    else a.init.flatMap(n => Seq(n, separator)) :+ a.last
