package works.iterative.ui.components
package tailwind

import com.raquo.laminar.api.L.svg.*
import com.raquo.laminar.api.L.SvgElement
import com.raquo.laminar.codecs.StringAsIsCodec

object Icons:
    object aria:
        val hidden = laminar.CustomAttrs.svg.ariaHidden

    inline def spinner(extraClasses: String): SvgElement =
        svg(
            cls(extraClasses),
            svgAttr("role", StringAsIsCodec, None) := "status",
            cls := "inline mr-2 text-gray-200 animate-spin dark:text-gray-600 fill-indigo-600",
            viewBox := "0 0 100 101",
            fill := "none",
            xmlns := "http://www.w3.org/2000/svg",
            path(
                d := "M100 50.5908C100 78.2051 77.6142 100.591 50 100.591C22.3858 100.591 0 78.2051 0 50.5908C0 22.9766 22.3858 0.59082 50 0.59082C77.6142 0.59082 100 22.9766 100 50.5908ZM9.08144 50.5908C9.08144 73.1895 27.4013 91.5094 50 91.5094C72.5987 91.5094 90.9186 73.1895 90.9186 50.5908C90.9186 27.9921 72.5987 9.67226 50 9.67226C27.4013 9.67226 9.08144 27.9921 9.08144 50.5908Z",
                fill := "currentColor"
            ),
            path(
                d := "M93.9676 39.0409C96.393 38.4038 97.8624 35.9116 97.0079 33.5539C95.2932 28.8227 92.871 24.3692 89.8167 20.348C85.8452 15.1192 80.8826 10.7238 75.2124 7.41289C69.5422 4.10194 63.2754 1.94025 56.7698 1.05124C51.7666 0.367541 46.6976 0.446843 41.7345 1.27873C39.2613 1.69328 37.813 4.19778 38.4501 6.62326C39.0873 9.04874 41.5694 10.4717 44.0505 10.1071C47.8511 9.54855 51.7191 9.52689 55.5402 10.0491C60.8642 10.7766 65.9928 12.5457 70.6331 15.2552C75.2735 17.9648 79.3347 21.5619 82.5849 25.841C84.9175 28.9121 86.7997 32.2913 88.1811 35.8758C89.083 38.2158 91.5421 39.6781 93.9676 39.0409Z",
                fill := "currentFill"
            )
        )

    object outline:

        inline def bell(extraClasses: String): SvgElement =
            svg(
                cls(extraClasses),
                xmlns("http://www.w3.org/2000/svg"),
                fill("none"),
                viewBox("0 0 24 24"),
                stroke("currentColor"),
                aria.hidden(true),
                path(
                    strokeLineCap("round"),
                    strokeLineJoin("round"),
                    strokeWidth("2"),
                    d(
                        "M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"
                    )
                )
            )

        inline def `check-circle`(extraClasses: String): SvgElement =
            svg(
                cls(extraClasses),
                fill("none"),
                stroke("currentColor"),
                viewBox("0 0 24 24"),
                xmlns("http://www.w3.org/2000/svg"),
                path(
                    strokeLineCap("round"),
                    strokeLineJoin("round"),
                    strokeWidth("2"),
                    d("M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z")
                )
            )

        inline def `document-add`(extraClasses: String): SvgElement =
            svg(
                cls(extraClasses),
                fill("none"),
                stroke("currentColor"),
                viewBox("0 0 24 24"),
                xmlns("http://www.w3.org/2000/svg"),
                path(
                    strokeLineCap("round"),
                    strokeLineJoin("round"),
                    strokeWidth("2"),
                    d(
                        "M9 13h6m-3-3v6m5 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                    )
                )
            )

        inline def `external-link`(extraClasses: String): SvgElement =
            svg(
                cls(extraClasses),
                fill("none"),
                stroke("currentColor"),
                viewBox("0 0 24 24"),
                xmlns("http://www.w3.org/2000/svg"),
                path(
                    strokeLineCap("round"),
                    strokeLineJoin("round"),
                    strokeWidth("2"),
                    d(
                        "M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14"
                    )
                )
            )

        inline def menu(extraClasses: String): SvgElement =
            svg(
                cls(extraClasses),
                xmlns("http://www.w3.org/2000/svg"),
                fill("none"),
                viewBox("0 0 24 24"),
                stroke("currentColor"),
                aria.hidden(true),
                path(
                    strokeLineCap("round"),
                    strokeLineJoin("round"),
                    strokeWidth("2"),
                    d("M4 6h16M4 12h16M4 18h16")
                )
            )

        inline def `status-offline`(extraClasses: String): SvgElement =
            svg(
                cls(extraClasses),
                fill("none"),
                stroke("currentColor"),
                viewBox("0 0 24 24"),
                xmlns("http://www.w3.org/2000/svg"),
                aria.hidden(true),
                path(
                    strokeLineCap("round"),
                    strokeLineJoin("round"),
                    strokeWidth("2"),
                    d(
                        "M18.364 5.636a9 9 0 010 12.728m0 0l-2.829-2.829m2.829 2.829L21 21M15.536 8.464a5 5 0 010 7.072m0 0l-2.829-2.829m-4.243 2.829a4.978 4.978 0 01-1.414-2.83m-1.414 5.658a9 9 0 01-2.167-9.238m7.824 2.167a1 1 0 111.414 1.414m-1.414-1.414L3 3m8.293 8.293l1.414 1.414"
                    )
                )
            )

        inline def user(extraClasses: String): SvgElement =
            svg(
                cls(extraClasses),
                fill("none"),
                stroke("currentColor"),
                viewBox("0 0 24 24"),
                xmlns("http://www.w3.org/2000/svg"),
                aria.hidden(true),
                path(
                    strokeLineCap("round"),
                    strokeLineJoin("round"),
                    strokeWidth("2"),
                    d(
                        "M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"
                    )
                )
            )

        inline def x(extraClasses: String, sw: String = "2"): SvgElement =
            svg(
                cls(extraClasses),
                xmlns("http://www.w3.org/2000/svg"),
                fill("none"),
                viewBox("0 0 24 24"),
                stroke("currentColor"),
                aria.hidden(true),
                path(
                    strokeLineCap("round"),
                    strokeLineJoin("round"),
                    strokeWidth(sw),
                    d("M6 18L18 6M6 6l12 12")
                )
            )

        inline def calendar(extraClasses: String): SvgElement =
            svg(
                cls(extraClasses),
                fill := "none",
                stroke := "currentColor",
                viewBox := "0 0 24 24",
                xmlns := "http://www.w3.org/2000/svg",
                path(
                    strokeLineCap := "round",
                    strokeLineJoin := "round",
                    strokeWidth := "2",
                    d := "M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"
                )
            )

        inline def document(extraClasses: String): SvgElement =
            svg(
                cls(extraClasses),
                fill := "none",
                stroke := "currentColor",
                viewBox := "0 0 24 24",
                xmlns := "http://www.w3.org/2000/svg",
                path(
                    strokeLineCap := "round",
                    strokeLineJoin := "round",
                    strokeWidth := "2",
                    d := "M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z"
                )
            )

    end outline

    object solid:

        inline def annotation(extraClasses: String): SvgElement =
            svg(
                cls(extraClasses),
                fill := "currentColor",
                viewBox := "0 0 20 20",
                xmlns := "http://www.w3.org/2000/svg",
                path(
                    fillRule := "evenodd",
                    d := "M18 13V5a2 2 0 00-2-2H4a2 2 0 00-2 2v8a2 2 0 002 2h3l3 3 3-3h3a2 2 0 002-2zM5 7a1 1 0 011-1h8a1 1 0 110 2H6a1 1 0 01-1-1zm1 3a1 1 0 100 2h3a1 1 0 100-2H6z",
                    clipRule := "evenodd"
                )
            )

        inline def exclamation(extraClasses: String): SvgElement =
            svg(
                cls := extraClasses,
                xmlns := "http://www.w3.org/2000/svg",
                viewBox := "0 0 20 20",
                fill := "currentColor",
                aria.hidden(true),
                path(
                    fillRule := "evenodd",
                    d := "M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z",
                    clipRule := "evenodd"
                )
            )

        inline def users(extraClasses: String): SvgElement =
            svg(
                cls(extraClasses),
                xmlns("http://www.w3.org/2000/svg"),
                viewBox("0 0 20 20"),
                fill("currentColor"),
                aria.hidden(true),
                path(
                    d(
                        "M9 6a3 3 0 11-6 0 3 3 0 016 0zM17 6a3 3 0 11-6 0 3 3 0 016 0zM12.93 17c.046-.327.07-.66.07-1a6.97 6.97 0 00-1.5-4.33A5 5 0 0119 16v1h-6.07zM6 11a5 5 0 015 5v1H1v-1a5 5 0 015-5z"
                    )
                )
            )

        inline def `location-marker`(extraClasses: String): SvgElement =
            svg(
                cls(extraClasses),
                xmlns("http://www.w3.org/2000/svg"),
                viewBox("0 0 20 20"),
                fill("currentColor"),
                aria.hidden(true),
                path(
                    fillRule("evenodd"),
                    d(
                        "M5.05 4.05a7 7 0 119.9 9.9L10 18.9l-4.95-4.95a7 7 0 010-9.9zM10 11a2 2 0 100-4 2 2 0 000 4z"
                    ),
                    clipRule("evenodd")
                )
            )

        inline def calendar(extraClasses: String): SvgElement =
            svg(
                cls(extraClasses),
                xmlns("http://www.w3.org/2000/svg"),
                viewBox("0 0 20 20"),
                fill("currentColor"),
                aria.hidden(true),
                path(
                    fillRule("evenodd"),
                    d(
                        "M6 2a1 1 0 00-1 1v1H4a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-1V3a1 1 0 10-2 0v1H7V3a1 1 0 00-1-1zm0 5a1 1 0 000 2h8a1 1 0 100-2H6z"
                    ),
                    clipRule("evenodd")
                )
            )

        inline def `chevron-right`(extraClasses: String): SvgElement =
            svg(
                cls(extraClasses),
                xmlns("http://www.w3.org/2000/svg"),
                viewBox("0 0 20 20"),
                fill("currentColor"),
                aria.hidden(true),
                path(
                    fillRule("evenodd"),
                    d(
                        "M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z"
                    ),
                    clipRule("evenodd")
                )
            )

        inline def search(extraClasses: String): SvgElement =
            svg(
                cls(extraClasses),
                xmlns("http://www.w3.org/2000/svg"),
                viewBox("0 0 20 20"),
                fill("currentColor"),
                aria.hidden(true),
                path(
                    fillRule("evenodd"),
                    d(
                        "M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z"
                    ),
                    clipRule("evenodd")
                )
            )

        inline def filter(extraClasses: String): SvgElement =
            svg(
                cls(extraClasses),
                xmlns("http://www.w3.org/2000/svg"),
                viewBox("0 0 20 20"),
                fill("currentColor"),
                aria.hidden(true),
                path(
                    fillRule("evenodd"),
                    d(
                        "M3 3a1 1 0 011-1h12a1 1 0 011 1v3a1 1 0 01-.293.707L12 11.414V15a1 1 0 01-.293.707l-2 2A1 1 0 018 17v-5.586L3.293 6.707A1 1 0 013 6V3z"
                    ),
                    clipRule("evenodd")
                )
            )

        inline def `arrow-narrow-left`(extraClasses: String): SvgElement =
            svg(
                cls(extraClasses),
                xmlns("http://www.w3.org/2000/svg"),
                viewBox("0 0 20 20"),
                fill("currentColor"),
                aria.hidden(true),
                path(
                    fillRule("evenodd"),
                    d(
                        "M7.707 14.707a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414l4-4a1 1 0 011.414 1.414L5.414 9H17a1 1 0 110 2H5.414l2.293 2.293a1 1 0 010 1.414z"
                    ),
                    clipRule("evenodd")
                )
            )

        inline def home(extraClasses: String): SvgElement =
            svg(
                cls(extraClasses),
                xmlns("http://www.w3.org/2000/svg"),
                viewBox("0 0 20 20"),
                fill("currentColor"),
                aria.hidden(true),
                path(
                    d(
                        "M10.707 2.293a1 1 0 00-1.414 0l-7 7a1 1 0 001.414 1.414L4 10.414V17a1 1 0 001 1h2a1 1 0 001-1v-2a1 1 0 011-1h2a1 1 0 011 1v2a1 1 0 001 1h2a1 1 0 001-1v-6.586l.293.293a1 1 0 001.414-1.414l-7-7z"
                    )
                )
            )

        inline def paperclip(extraClasses: String): SvgElement =
            svg(
                cls(extraClasses),
                xmlns("http://www.w3.org/2000/svg"),
                viewBox("0 0 20 20"),
                fill("currentColor"),
                aria.hidden(true),
                path(
                    fillRule("evenodd"),
                    d(
                        "M8 4a3 3 0 00-3 3v4a5 5 0 0010 0V7a1 1 0 112 0v4a7 7 0 11-14 0V7a5 5 0 0110 0v4a3 3 0 11-6 0V7a1 1 0 012 0v4a1 1 0 102 0V7a3 3 0 00-3-3z"
                    ),
                    clipRule("evenodd")
                )
            )

        inline def info(extraClasses: String): SvgElement =
            svg(
                cls(extraClasses),
                fill := "currentColor",
                viewBox := "0 0 20 20",
                xmlns := "http://www.w3.org/2000/svg",
                path(
                    fillRule := "evenodd",
                    d := "M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z",
                    clipRule := "evenodd"
                )
            )

        inline def `dots-circle-horizontal`(extraClasses: String): SvgElement =
            svg(
                cls := extraClasses,
                fill := "currentColor",
                viewBox := "0 0 20 20",
                xmlns := "http://www.w3.org/2000/svg",
                path(
                    fillRule := "evenodd",
                    d := "M10 18a8 8 0 100-16 8 8 0 000 16zM7 9H5v2h2V9zm8 0h-2v2h2V9zM9 9h2v2H9V9z",
                    clipRule := "evenodd"
                )
            )

        inline def `x-circle`(extraClasses: String): SvgElement =
            svg(
                cls := extraClasses,
                xmlns := "http://www.w3.org/2000/svg",
                viewBox := "0 0 20 20",
                fill := "currentColor",
                aria.hidden := true,
                path(
                    fillRule := "evenodd",
                    d := "M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z",
                    clipRule := "evenodd"
                )
            )

        inline def `check-circle`(extraClasses: String): SvgElement =
            svg(
                cls := extraClasses,
                xmlns := "http://www.w3.org/2000/svg",
                viewBox := "0 0 20 20",
                fill := "currentColor",
                aria.hidden := true,
                path(
                    fillRule := "evenodd",
                    d := "M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z",
                    clipRule := "evenodd"
                )
            )

    end solid
end Icons
