package cz.e_bs.cmi.mdr.pdb.app.components

import com.raquo.domtypes.generic.codecs.BooleanAsTrueFalseStringCodec
import com.raquo.domtypes.generic.defs.attrs.AriaAttrs
import com.raquo.laminar.api.L.svg.{*, given}
import com.raquo.laminar.builders.SvgBuilders
import com.raquo.laminar.keys.ReactiveSvgAttr

object Icons:
  val defaultSize: Int = 6

  // TODO: remove aria-hidden from here, move to call sites, it has no reason to be here
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
        aria.hidden := true,
        path(
          strokeLineCap := "round",
          strokeLineJoin := "round",
          strokeWidth := "2",
          d := "M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"
        )
      )

  end outline

  object solid:
    def users =
      svg(
        cls := "flex-shrink-0 mr-1.5 h-5 w-5 text-gray-400",
        xmlns := "http://www.w3.org/2000/svg",
        viewBox := "0 0 20 20",
        fill := "currentColor",
        aria.hidden := true,
        path(
          d := "M9 6a3 3 0 11-6 0 3 3 0 016 0zM17 6a3 3 0 11-6 0 3 3 0 016 0zM12.93 17c.046-.327.07-.66.07-1a6.97 6.97 0 00-1.5-4.33A5 5 0 0119 16v1h-6.07zM6 11a5 5 0 015 5v1H1v-1a5 5 0 015-5z"
        )
      )

    def `location-marker` =
      svg(
        cls := "flex-shrink-0 mr-1.5 h-5 w-5 text-gray-400",
        xmlns := "http://www.w3.org/2000/svg",
        viewBox := "0 0 20 20",
        fill := "currentColor",
        aria.hidden := true,
        path(
          fillRule := "evenodd",
          d := "M5.05 4.05a7 7 0 119.9 9.9L10 18.9l-4.95-4.95a7 7 0 010-9.9zM10 11a2 2 0 100-4 2 2 0 000 4z",
          clipRule := "evenodd"
        )
      )

    def calendar =
      svg(
        cls := "flex-shrink-0 mr-1.5 h-5 w-5 text-gray-400",
        xmlns := "http://www.w3.org/2000/svg",
        viewBox := "0 0 20 20",
        fill := "currentColor",
        aria.hidden := true,
        path(
          fillRule := "evenodd",
          d := "M6 2a1 1 0 00-1 1v1H4a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-1V3a1 1 0 10-2 0v1H7V3a1 1 0 00-1-1zm0 5a1 1 0 000 2h8a1 1 0 100-2H6z",
          clipRule := "evenodd"
        )
      )

    def `chevron-right` =
      svg(
        cls := "h-5 w-5 text-gray-400",
        xmlns := "http://www.w3.org/2000/svg",
        viewBox := "0 0 20 20",
        fill := "currentColor",
        aria.hidden := true,
        path(
          fillRule := "evenodd",
          d := "M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z",
          clipRule := "evenodd"
        )
      )
  end solid
end Icons
