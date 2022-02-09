package cz.e_bs.cmi.mdr.pdb.app.components

import com.raquo.domtypes.generic.codecs.BooleanAsTrueFalseStringCodec
import com.raquo.domtypes.generic.defs.attrs.AriaAttrs
import com.raquo.laminar.api.L.svg.{*, given}
import com.raquo.laminar.builders.SvgBuilders
import com.raquo.laminar.keys.ReactiveSvgAttr

object Icons:
  val defaultSize: Int = 6

  object aria:
    val hidden = customSvgAttr("aria-hidden", BooleanAsTrueFalseStringCodec)

  object outline:
    def bell =
      svg(
        cls := "h-6 w-6",
        xmlns := "http://www.w3.org/2000/svg",
        fill := "none",
        viewBox := "0 0 24 24",
        stroke := "currentColor",
        aria.hidden := true,
        path(
          strokeLineCap := "round",
          strokeLineJoin := "round",
          strokeWidth := "2",
          d := "M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"
        )
      )

    def menu = svg(
      cls := "h-6 w-6",
      xmlns := "http://www.w3.org/2000/svg",
      fill := "none",
      viewBox := "0 0 24 24",
      stroke := "currentColor",
      aria.hidden := true,
      path(
        strokeLineCap := "round",
        strokeLineJoin := "round",
        strokeWidth := "2",
        d := "M4 6h16M4 12h16M4 18h16"
      )
    )

    def x =
      svg(
        cls := "h-6 w-6",
        xmlns := "http://www.w3.org/2000/svg",
        fill := "none",
        viewBox := "0 0 24 24",
        stroke := "currentColor",
        aria.hidden := true,
        path(
          strokeLineCap := "round",
          strokeLineJoin := "round",
          strokeWidth := "2",
          d := "M6 18L18 6M6 6l12 12"
        )
      )

    inline def user(size: Int = Icons.defaultSize) =
      svg(
        cls := s"w-${size} h-${size}",
        fill := "none",
        stroke := "currentColor",
        viewBox := "0 0 24 24",
        xmlns := "http://www.w3.org/2000/svg",
        path(
          strokeLineCap := "round",
          strokeLineJoin := "round",
          strokeWidth := "2",
          d := "M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"
        )
      )

  end outline
end Icons
