package works.iterative.ui.components.tailwind

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import java.time.LocalDate
import works.iterative.core.PlainMultiLine
import java.time.Instant

trait HtmlRenderable[A]:
  def toHtml(a: A): Node
  extension (a: A) def render: Node = toHtml(a)

object HtmlRenderable:
  given elementValue: HtmlRenderable[HtmlElement] with
    def toHtml(a: HtmlElement): Node = a
  given stringValue: HtmlRenderable[String] with
    def toHtml(v: String): Node =
      com.raquo.laminar.nodes.TextNode(v)
  given dateValue: HtmlRenderable[LocalDate] with
    def toHtml(v: LocalDate): Node =
      TimeUtils.formatDate(v)
  given instantValue: HtmlRenderable[Instant] with
    def toHtml(v: Instant): Node =
      TimeUtils.formatDateTime(v)
  given plainMultiLineValue: HtmlRenderable[PlainMultiLine] with
    def toHtml(v: PlainMultiLine): Node =
      p(
        v.split("\n")
          .map(t => Seq(com.raquo.laminar.nodes.TextNode(t), br()))
          .flatten: _*
      )
