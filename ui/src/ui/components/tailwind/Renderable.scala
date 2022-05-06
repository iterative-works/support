package works.iterative.ui.components.tailwind

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import java.time.LocalDate

trait HtmlRenderable[A]:
  extension (a: A) def render: Modifier[HtmlElement]

object HtmlRenderable:
  given stringValue: HtmlRenderable[String] with
    extension (v: String)
      def render: Modifier[HtmlElement] =
        v: Modifier[HtmlElement]
  given dateValue: HtmlRenderable[LocalDate] with
    extension (v: LocalDate)
      def render: Modifier[HtmlElement] =
        TimeUtils.formatDate(v)
