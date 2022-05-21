package works.iterative.ui.components.tailwind

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import java.time.LocalDate
import works.iterative.core.PlainMultiLine
import java.time.Instant

trait HtmlRenderable[A]:
  def toHtml(a: A): Modifier[HtmlElement]
  extension (a: A) def render: Modifier[HtmlElement] = toHtml(a)

object HtmlRenderable:
  given stringValue: HtmlRenderable[String] with
    def toHtml(v: String): Modifier[HtmlElement] =
      com.raquo.laminar.nodes.TextNode(v)
  given dateValue: HtmlRenderable[LocalDate] with
    def toHtml(v: LocalDate): Modifier[HtmlElement] =
      TimeUtils.formatDate(v)
  given instantValue: HtmlRenderable[Instant] with
    def toHtml(v: Instant): Modifier[HtmlElement] =
      TimeUtils.formatDateTime(v)
  given plainMultiLineValue: HtmlRenderable[PlainMultiLine] with
    def toHtml(v: PlainMultiLine): Modifier[HtmlElement] =
      p(
        v.split("\n")
          .map(t => Seq(com.raquo.laminar.nodes.TextNode(t), br()))
          .flatten: _*
      )
