package works.iterative.ui.components.laminar

import com.raquo.laminar.api.L.{*, given}
import works.iterative.ui.components.tailwind.ComponentContext
import com.raquo.domtypes.generic.codecs.BooleanAsTrueFalseStringCodec
import works.iterative.ui.components.tailwind.CustomAttrs

trait IconsModule:
  def icons: Icons

  trait Icons:
    def avatarPlaceholder(mods: Modifier[SvgElement]*): SvgElement
    def close(mods: Modifier[SvgElement]*): SvgElement
    def `search-solid`(mods: Modifier[SvgElement]*): SvgElement
    def `filter-solid`(mods: Modifier[SvgElement]*): SvgElement
    def `document-chart-bar-outline`(mods: Modifier[SvgElement]*): SvgElement
    def `paper-clip-solid`(mods: Modifier[SvgElement]*): SvgElement

trait DefaultIconsModule(using ComponentContext) extends IconsModule:
  override val icons: Icons = new Icons:
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

    override def close(mods: Modifier[SvgElement]*): SvgElement =
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

    override def `search-solid`(mods: Modifier[SvgElement]*): SvgElement =
      svg(
        withDefault(mods, cls("h-5 w-5 text-gray-400")),
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

    override def `filter-solid`(mods: Modifier[SvgElement]*): SvgElement =
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

    override def `document-chart-bar-outline`(
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

    override def `paper-clip-solid`(mods: Modifier[SvgElement]*): SvgElement =
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
