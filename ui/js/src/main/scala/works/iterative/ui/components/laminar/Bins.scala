package works.iterative.ui.components.laminar

import com.raquo.laminar.api.L.*
import com.raquo.laminar.tags.HtmlTag

case class Bin[Source, +Value](
    label: String,
    description: Option[String | HtmlElement],
    valueOf: Source => Value
):
    def map[NewValue](
        f: Bin[Source, Value] => Source => NewValue
    ): Bin[Source, NewValue] =
        copy(valueOf = f(this))
end Bin

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
end Bins
