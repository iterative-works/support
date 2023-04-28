package works.iterative.ui.components.tailwind
package laminar

import com.raquo.laminar.api.L.{*, given}
import works.iterative.ui.model.color.Color

object LaminarExtensions:
  given colorToCSS: Conversion[Color, Modifier[HtmlElement]] with
    def apply(c: Color) = cls(c.toCSS)

  given colorToSVGCSS: Conversion[Color, Modifier[SvgElement]] with
    def apply(c: Color) = svg.cls(c.toCSS)

  given colorSignalToCSS: Conversion[Signal[Color], Modifier[HtmlElement]] with
    def apply(c: Signal[Color]) = cls <-- c.map(_.toCSS)

  given colorSignalToSVGCSS: Conversion[Signal[Color], Modifier[SvgElement]]
    with
    def apply(c: Signal[Color]) = svg.cls <-- c.map(_.toCSS)
