package works.iterative.ui.components.laminar.tailwind.ui

import com.raquo.laminar.api.L.*
import com.raquo.laminar.codecs
import works.iterative.ui.components.laminar.CustomAttrs

trait IconsModule:
  object icons:
    import svg.*

    object aria:
      val hidden = CustomAttrs.svg.ariaHidden

    private def withDefault(
        mods: Seq[Modifier[SvgElement]],
        default: Modifier[SvgElement]
    ): Modifier[SvgElement] =
      if mods.isEmpty then default else mods

    def avatarPlaceholder(mods: Modifier[SvgElement]*): SvgElement =
      svg(
        withDefault(mods, cls := "h-8 w-8"),
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

    def close(mods: Modifier[SvgElement]*): SvgElement =
      svg(
        withDefault(mods, cls := "h-2 w-2"),
        stroke := "currentColor",
        fill := "none",
        viewBox := "0 0 8 8",
        path(
          strokeLineCap := "round",
          strokeWidth := "1.5",
          d := "M1 1l6 6m0-6L1 7"
        )
      )

    def upload(mods: SvgMod*): SvgElement =
      svg(
        withDefault(mods, cls("w-6 h-6")),
        fill("currentColor"),
        viewBox("0 0 24 24"),
        path(d := "M0 0h24v24H0z", fill("none")),
        path(d := "M9 16h6v-6h4l-7-7-7 7h4zm-4 2h14v2H5z")
      )

    def home(mods: SvgMod*): SvgElement =
      svg(
        withDefault(mods, cls("w-6 h-6")),
        fill("currentColor"),
        viewBox("0 0 20 20"),
        xmlns("http://www.w3.org/2000/svg"),
        aria.hidden(true),
        path(
          clipRule := "evenodd",
          fillRule := "evenodd",
          d := "M9.293 2.293a1 1 0 011.414 0l7 7A1 1 0 0117 11h-1v6a1 1 0 01-1 1h-2a1 1 0 01-1-1v-3a1 1 0 00-1-1H9a1 1 0 00-1 1v3a1 1 0 01-1 1H5a1 1 0 01-1-1v-6H3a1 1 0 01-.707-1.707l7-7z"
        )
      )

    def `x-mark-outline`(mods: SvgMod*): SvgElement =
      svg(
        withDefault(mods, cls("w-6 h-6")),
        fill("none"),
        viewBox("0 0 24 24"),
        strokeWidth("1.5"),
        stroke("currentColor"),
        path(
          strokeLineCap := "round",
          strokeLineJoin := "round",
          d := "M6 18L18 6M6 6l12 12"
        )
      )

    def `arrow-left-outline`(mods: SvgMod*): SvgElement =
      svg(
        withDefault(mods, cls("w-6 h-6")),
        fill("none"),
        viewBox("0 0 24 24"),
        strokeWidth("1.5"),
        stroke("currentColor"),
        path(
          strokeLineCap("round"),
          strokeLineJoin("round"),
          d("M10.5 19.5L3 12m0 0l7.5-7.5M3 12h18")
        )
      )

    def `chevron-left-outline`(mods: SvgMod*): SvgElement =
      svg(
        withDefault(mods, cls("w-6 h-6")),
        fill("none"),
        viewBox("0 0 24 24"),
        strokeWidth("1.5"),
        stroke("currentColor"),
        path(
          strokeLineCap("round"),
          strokeLineJoin("round"),
          d("M15.75 19.5L8.25 12l7.5-7.5")
        )
      )

    def `chevron-right-solid`(mods: SvgMod*): SvgElement =
      svg(
        withDefault(mods, cls("h-5 w-5")),
        viewBox("0 0 20 20"),
        fill("currentColor"),
        aria.hidden(true),
        path(
          clipRule("evenodd"),
          fillRule("evenodd"),
          d(
            "M7.21 14.77a.75.75 0 01.02-1.06L11.168 10 7.23 6.29a.75.75 0 111.04-1.08l4.5 4.25a.75.75 0 010 1.08l-4.5 4.25a.75.75 0 01-1.06-.02z"
          )
        )
      )

    def `search-solid`(mods: Modifier[SvgElement]*): SvgElement =
      svg(
        withDefault(mods, cls("h-5 w-5 text-gray-400")),
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

    def `filter-solid`(mods: Modifier[SvgElement]*): SvgElement =
      svg(
        withDefault(mods, cls("h-5 w-5 text-gray-400")),
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

    def `document-chart-bar-outline`(
        mods: Modifier[SvgElement]*
    ): SvgElement =
      svg(
        xmlns := "http://www.w3.org/2000/svg",
        fill := "none",
        viewBox := "0 0 24 24",
        strokeWidth := "1.5",
        stroke := "currentColor",
        withDefault(mods, cls := "h-6 w-6"),
        path(
          strokeLineCap := "round",
          strokeLineJoin := "round",
          d := "M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25M9 16.5v.75m3-3v3M15 12v5.25m-4.5-15H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z"
        )
      )

    def `paper-clip-solid`(mods: Modifier[SvgElement]*): SvgElement =
      svg(
        withDefault(mods, cls := "h-5 w-5"),
        cls := "flex-shrink-0 text-gray-400",
        viewBox := "0 0 20 20",
        fill := "currentColor",
        aria.hidden := true,
        path(
          fillRule := "evenodd",
          d := "M15.621 4.379a3 3 0 00-4.242 0l-7 7a3 3 0 004.241 4.243h.001l.497-.5a.75.75 0 011.064 1.057l-.498.501-.002.002a4.5 4.5 0 01-6.364-6.364l7-7a4.5 4.5 0 016.368 6.36l-3.455 3.553A2.625 2.625 0 119.52 9.52l3.45-3.451a.75.75 0 111.061 1.06l-3.45 3.451a1.125 1.125 0 001.587 1.595l3.454-3.553a3 3 0 000-4.242z",
          clipRule := "evenodd"
        )
      )

    def `exclamation-circle-solid`(mods: SvgMod*): SvgElement =
      svg(
        withDefault(mods, cls := "h-5 w-5"),
        viewBox("0 0 20 20"),
        fill("currentColor"),
        aria.hidden(true),
        path(
          fillRule("evenodd"),
          d(
            "M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-8-5a.75.75 0 01.75.75v4.5a.75.75 0 01-1.5 0v-4.5A.75.75 0 0110 5zm0 10a1 1 0 100-2 1 1 0 000 2z"
          ),
          clipRule("evenodd")
        )
      )

    def `alert-warning`(mods: SvgMod*): SvgElement =
      svg(
        withDefault(mods, cls("h-5 w-5 text-yellow-400")),
        viewBox("0 0 20 20"),
        fill("currentColor"),
        aria.hidden(true),
        path(
          fillRule("evenodd"),
          d(
            "M8.485 2.495c.673-1.167 2.357-1.167 3.03 0l6.28 10.875c.673 1.167-.17 2.625-1.516 2.625H3.72c-1.347 0-2.189-1.458-1.515-2.625L8.485 2.495zM10 5a.75.75 0 01.75.75v3.5a.75.75 0 01-1.5 0v-3.5A.75.75 0 0110 5zm0 9a1 1 0 100-2 1 1 0 000 2z"
          ),
          clipRule("evenodd")
        )
      )

    def `alert-error`(mods: SvgMod*): SvgElement =
      svg(
        withDefault(mods, cls := "h-5 w-5 text-red-400"),
        viewBox := "0 0 20 20",
        fill := "currentColor",
        aria.hidden := true,
        path(
          fillRule := "evenodd",
          d := "M10 18a8 8 0 100-16 8 8 0 000 16zM8.28 7.22a.75.75 0 00-1.06 1.06L8.94 10l-1.72 1.72a.75.75 0 101.06 1.06L10 11.06l1.72 1.72a.75.75 0 101.06-1.06L11.06 10l1.72-1.72a.75.75 0 00-1.06-1.06L10 8.94 8.28 7.22z",
          clipRule := "evenodd"
        )
      )

    def `alert-success`(mods: SvgMod*): SvgElement =
      svg(
        withDefault(mods, cls := "h-5 w-5 text-green-400"),
        viewBox := "0 0 20 20",
        fill := "currentColor",
        aria.hidden := true,
        path(
          fillRule := "evenodd",
          d := "M10 18a8 8 0 100-16 8 8 0 000 16zm3.857-9.809a.75.75 0 00-1.214-.882l-3.483 4.79-1.88-1.88a.75.75 0 10-1.06 1.061l2.5 2.5a.75.75 0 001.137-.089l4-5.5z",
          clipRule := "evenodd"
        )
      )

    def `alert-info`(mods: SvgMod*): SvgElement =
      svg(
        withDefault(mods, cls := "h-5 w-5 text-blue-400"),
        viewBox := "0 0 20 20",
        fill := "currentColor",
        aria.hidden := true,
        path(
          fillRule := "evenodd",
          d := "M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a.75.75 0 000 1.5h.253a.25.25 0 01.244.304l-.459 2.066A1.75 1.75 0 0010.747 15H11a.75.75 0 000-1.5h-.253a.25.25 0 01-.244-.304l.459-2.066A1.75 1.75 0 009.253 9H9z",
          clipRule := "evenodd"
        )
      )

    def spinner(mods: SvgMod*): SvgElement = svg(
      withDefault(mods, cls("h-4 w-4")),
      svgAttr("role", codecs.StringAsIsCodec, None) := "status",
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

    def arrowPath(mods: SvgMod*): SvgElement =
      svg(
        withDefault(mods, cls("h-5 w-5")),
        fill("none"),
        stroke("currentColor"),
        strokeWidth("1.5"),
        viewBox("0 0 24 24"),
        xmlns("http://www.w3.org/2000/svg"),
        aria.hidden(true),
        path(
          strokeLineCap("round"),
          strokeLineJoin("round"),
          d(
            "M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M4.031 9.865a8.25 8.25 0 0113.803-3.7l3.181 3.182m0-4.991v4.99"
          )
        )
      )

    def chevronUpDown(mods: SvgMod*): SvgElement =
      svg(
        withDefault(mods, cls("h-5 w-5")),
        viewBox := "0 0 20 20",
        fill := "currentColor",
        aria.hidden := true,
        path(
          fillRule := "evenodd",
          d := "M10 3a.75.75 0 01.55.24l3.25 3.5a.75.75 0 11-1.1 1.02L10 4.852 7.3 7.76a.75.75 0 01-1.1-1.02l3.25-3.5A.75.75 0 0110 3zm-3.76 9.2a.75.75 0 011.06.04l2.7 2.908 2.7-2.908a.75.75 0 111.1 1.02l-3.25 3.5a.75.75 0 01-1.1 0l-3.25-3.5a.75.75 0 01.04-1.06z",
          clipRule := "evenodd"
        )
      )

    def check(mods: SvgMod*): SvgElement =
      svg(
        withDefault(mods, cls("h-5 w-5")),
        viewBox := "0 0 20 20",
        fill := "currentColor",
        aria.hidden := true,
        path(
          fillRule := "evenodd",
          d := "M16.704 4.153a.75.75 0 01.143 1.052l-8 10.5a.75.75 0 01-1.127.075l-4.5-4.5a.75.75 0 011.06-1.06l3.894 3.893 7.48-9.817a.75.75 0 011.05-.143z",
          clipRule := "evenodd"
        )
      )
