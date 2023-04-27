package works.iterative.ui.components.tailwind
package laminar

import com.raquo.laminar.api.L.{*, given}

object LaminarExtensions:
  given colorToCSS: Conversion[experimental.Color, Modifier[HtmlElement]] with
    def apply(c: experimental.Color) = cls(c.toCSS)

  given colorToSVGCSS: Conversion[experimental.Color, Modifier[SvgElement]] with
    def apply(c: experimental.Color) = svg.cls(c.toCSS)

  given colorSignalToCSS
      : Conversion[Signal[experimental.Color], Modifier[HtmlElement]] with
    def apply(c: Signal[experimental.Color]) = cls <-- c.map(_.toCSS)

  given colorSignalToSVGCSS
      : Conversion[Signal[experimental.Color], Modifier[SvgElement]] with
    def apply(c: Signal[experimental.Color]) = svg.cls <-- c.map(_.toCSS)
