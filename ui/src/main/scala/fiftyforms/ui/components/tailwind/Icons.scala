package works.iterative.ui.components.tailwind

import com.raquo.domtypes.generic.codecs.BooleanAsTrueFalseStringCodec
import com.raquo.domtypes.generic.defs.attrs.AriaAttrs
import com.raquo.laminar.api.L.svg.{*, given}
import com.raquo.laminar.builders.SvgBuilders
import com.raquo.laminar.keys.ReactiveSvgAttr
import works.iterative.ui.components.tailwind.Macros

// TODO: fix sizes, colors, hover and stuff, normalize and amend on call site
object Icons:
  object aria:
    inline def hidden = CustomAttrs.svg.ariaHidden

  object outline:
    val defaultSize: Int = 6

    inline def bell(size: Int = defaultSize) =
      svg(
        cls := Macros.size(size),
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

    inline def `check-circle`(size: Int = defaultSize) =
      svg(
        cls := Macros.size(size),
        fill := "none",
        stroke := "currentColor",
        viewBox := "0 0 24 24",
        xmlns := "http://www.w3.org/2000/svg",
        path(
          strokeLineCap := "round",
          strokeLineJoin := "round",
          strokeWidth := "2",
          d := "M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
        )
      )

    inline def `document-add`(size: Int = defaultSize) =
      svg(
        cls := Macros.size(size),
        fill := "none",
        stroke := "currentColor",
        viewBox := "0 0 24 24",
        xmlns := "http://www.w3.org/2000/svg",
        path(
          strokeLineCap := "round",
          strokeLineJoin := "round",
          strokeWidth := "2",
          d := "M9 13h6m-3-3v6m5 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
        )
      )

    inline def `external-link`(size: Int = defaultSize) =
      svg(
        cls := Macros.size(size),
        fill := "none",
        stroke := "currentColor",
        viewBox := "0 0 24 24",
        xmlns := "http://www.w3.org/2000/svg",
        path(
          strokeLineCap := "round",
          strokeLineJoin := "round",
          strokeWidth := "2",
          d := "M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14"
        )
      )

    inline def menu(size: Int = defaultSize) =
      svg(
        cls := Macros.size(size),
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

    inline def `status-offline`(size: Int = defaultSize) =
      svg(
        cls := Macros.size(size),
        fill := "none",
        stroke := "currentColor",
        viewBox := "0 0 24 24",
        xmlns := "http://www.w3.org/2000/svg",
        aria.hidden := true,
        path(
          strokeLineCap := "round",
          strokeLineJoin := "round",
          strokeWidth := "2",
          d := "M18.364 5.636a9 9 0 010 12.728m0 0l-2.829-2.829m2.829 2.829L21 21M15.536 8.464a5 5 0 010 7.072m0 0l-2.829-2.829m-4.243 2.829a4.978 4.978 0 01-1.414-2.83m-1.414 5.658a9 9 0 01-2.167-9.238m7.824 2.167a1 1 0 111.414 1.414m-1.414-1.414L3 3m8.293 8.293l1.414 1.414"
        )
      )

    inline def user(size: Int = defaultSize) =
      svg(
        cls := Macros.size(size),
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

    inline def x(size: Int = defaultSize) =
      svg(
        cls := Macros.size(size),
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

  end outline

  object solid:
    val defaultSize: Int = 5

    inline def users(size: Int = defaultSize) =
      svg(
        cls := Macros.size(size),
        xmlns := "http://www.w3.org/2000/svg",
        viewBox := "0 0 20 20",
        fill := "currentColor",
        aria.hidden := true,
        path(
          d := "M9 6a3 3 0 11-6 0 3 3 0 016 0zM17 6a3 3 0 11-6 0 3 3 0 016 0zM12.93 17c.046-.327.07-.66.07-1a6.97 6.97 0 00-1.5-4.33A5 5 0 0119 16v1h-6.07zM6 11a5 5 0 015 5v1H1v-1a5 5 0 015-5z"
        )
      )

    inline def `location-marker`(size: Int = defaultSize) =
      svg(
        cls := Macros.size(size),
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

    inline def calendar(size: Int = defaultSize) =
      svg(
        cls := Macros.size(size),
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

    inline def `chevron-right`(size: Int = defaultSize) =
      svg(
        cls := Macros.size(size),
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

    inline def search(size: Int = defaultSize) =
      svg(
        cls := Macros.size(size),
        xmlns := "http://www.w3.org/2000/svg",
        viewBox := "0 0 20 20",
        fill := "currentColor",
        aria.hidden := true,
        path(
          fillRule := "evenodd",
          d := "M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z",
          clipRule := "evenodd"
        )
      )

    inline def filter(size: Int = defaultSize) =
      svg(
        cls := Macros.size(size),
        xmlns := "http://www.w3.org/2000/svg",
        viewBox := "0 0 20 20",
        fill := "currentColor",
        aria.hidden := true,
        path(
          fillRule := "evenodd",
          d := "M3 3a1 1 0 011-1h12a1 1 0 011 1v3a1 1 0 01-.293.707L12 11.414V15a1 1 0 01-.293.707l-2 2A1 1 0 018 17v-5.586L3.293 6.707A1 1 0 013 6V3z",
          clipRule := "evenodd"
        )
      )

    inline def `arrow-narrow-left`(size: Int = defaultSize) =
      svg(
        cls := Macros.size(size),
        xmlns := "http://www.w3.org/2000/svg",
        viewBox := "0 0 20 20",
        fill := "currentColor",
        aria.hidden := true,
        path(
          fillRule := "evenodd",
          d := "M7.707 14.707a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414l4-4a1 1 0 011.414 1.414L5.414 9H17a1 1 0 110 2H5.414l2.293 2.293a1 1 0 010 1.414z",
          clipRule := "evenodd"
        )
      )

    inline def home(size: Int = defaultSize) =
      svg(
        cls := Macros.size(size),
        xmlns := "http://www.w3.org/2000/svg",
        viewBox := "0 0 20 20",
        fill := "currentColor",
        aria.hidden := true,
        path(
          d := "M10.707 2.293a1 1 0 00-1.414 0l-7 7a1 1 0 001.414 1.414L4 10.414V17a1 1 0 001 1h2a1 1 0 001-1v-2a1 1 0 011-1h2a1 1 0 011 1v2a1 1 0 001 1h2a1 1 0 001-1v-6.586l.293.293a1 1 0 001.414-1.414l-7-7z"
        )
      )

    inline def paperclip(size: Int = defaultSize) =
      svg(
        cls := Macros.size(size),
        xmlns := "http://www.w3.org/2000/svg",
        viewBox := "0 0 20 20",
        fill := "currentColor",
        aria.hidden := true,
        path(
          fillRule := "evenodd",
          d := "M8 4a3 3 0 00-3 3v4a5 5 0 0010 0V7a1 1 0 112 0v4a7 7 0 11-14 0V7a5 5 0 0110 0v4a3 3 0 11-6 0V7a1 1 0 012 0v4a1 1 0 102 0V7a3 3 0 00-3-3z",
          clipRule := "evenodd"
        )
      )

  end solid
end Icons
