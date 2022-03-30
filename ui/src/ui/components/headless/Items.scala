package works.iterative
package ui.components.headless

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement

class Items[A](
    frame: Modifier[HtmlElement] => HtmlElement,
    item: A => HtmlElement
):
  def apply(items: Seq[A]): HtmlElement = frame(items.map(a => li(item(a))))
  def contramap[B](f: B => A): Items[B] = Items(frame, b => item(f(b)))
  def mapItem(f: A => HtmlElement => HtmlElement): Items[A] =
    Items(frame, a => f(a)(item(a)))
