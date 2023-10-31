package works.iterative.ui.components.laminar.tailwind.ui

import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
import works.iterative.ui.components.laminar.tailwind.color.ColorKind
import works.iterative.ui.components.laminar.tailwind.color.ColorWeight

trait AlertComponentsModule:
  self: IconsModule =>
  object alerts:
    // TODO: use context functions and builder pattern to build the alerts
    def alert(
        message: HtmlMod,
        icon: SvgElement,
        color: ColorKind,
        title: Option[String] = None,
        actions: Option[ColorKind => HtmlMod] = None,
        mods: ColorKind => HtmlMod = _ => emptyMod
    ): HtmlElement =
      div(
        cls := "rounded-md p-4",
        color(50).bg,
        div(
          cls := "flex",
          icon,
          div(
            cls := "ml-3",
            title.map(t =>
              h3(
                cls := "text-sm font-medium",
                color(800).text,
                t
              )
            ),
            div(
              cls := "text-sm",
              title.map(_ => cls("mt-2")),
              color(700).text,
              message
            ),
            actions.map { act =>
              div(
                cls := "mt-4",
                act(color)
              )
            }
          ),
          mods(color)
        )
      )

    def warning(
        message: HtmlMod,
        title: Option[String] = None,
        actions: Option[ColorKind => HtmlMod] = None,
        mods: ColorKind => HtmlMod = _ => emptyMod
    ): HtmlElement =
      alert(
        message,
        icons.`alert-warning`(),
        ColorKind.yellow,
        title = title,
        actions = actions,
        mods = mods
      )

    def error(
        message: HtmlMod,
        title: Option[String] = None,
        actions: Option[ColorKind => HtmlMod] = None,
        mods: ColorKind => HtmlMod = _ => emptyMod
    ): HtmlElement =
      alert(
        message,
        icons.`alert-error`(),
        ColorKind.red,
        title = title,
        actions = actions,
        mods = mods
      )

    def success(
        message: HtmlMod,
        title: Option[String] = None,
        actions: Option[ColorKind => HtmlMod] = None,
        mods: ColorKind => HtmlMod = _ => emptyMod
    ): HtmlElement =
      alert(
        message,
        icons.`alert-success`(),
        ColorKind.green,
        title = title,
        actions = actions,
        mods = mods
      )

    def info(
        message: HtmlMod,
        title: Option[String] = None,
        actions: Option[ColorKind => HtmlMod] = None,
        mods: ColorKind => HtmlMod = _ => emptyMod
    ): HtmlElement =
      alert(
        message,
        icons.`alert-info`(),
        ColorKind.blue,
        title = title,
        actions = actions,
        mods = mods
      )

    def buttons(buttons: (ColorKind => HtmlMod)*): ColorKind => HtmlMod =
      color =>
        div(
          cls("-mx-2 -my-1.5 flex"),
          buttons.map(_(color))
        )

    def button(title: String)(mods: HtmlMod*): ColorKind => HtmlMod = color =>
      L.button(
        tpe("button"),
        cls(
          "first:ml-0 ml-3 rounded-md px-2 py-1.5 text-sm font-medium focus:outline-none focus:ring-2 focus:ring-offset-2"
        ),
        color(50).bg,
        color(800).text,
        cls(s"hover:${color(100).bg.toCSS}"),
        cls(s"focus:${color(600).ring.toCSS}"),
        cls(s"focus:${color(50).ringOffset.toCSS}"),
        title,
        mods
      )

    def closeMod(mods: HtmlMod): ColorKind => HtmlMod = color =>
      val closeIcon: SvgElement =
        import svg.*
        svg(
          cls := "h-5 w-5",
          viewBox := "0 0 20 20",
          fill := "currentColor",
          path(
            d := "M6.28 5.22a.75.75 0 00-1.06 1.06L8.94 10l-3.72 3.72a.75.75 0 101.06 1.06L10 11.06l3.72 3.72a.75.75 0 101.06-1.06L11.06 10l3.72-3.72a.75.75 0 00-1.06-1.06L10 8.94 6.28 5.22z"
          )
        )
      div(
        cls("ml-auto pl-3"),
        L.button(
          cls(
            "-mx-1.5 -my-1.5 inline-flex rounded-md p-1.5 focus:outline-none focus:ring-2 focus:ring-offset-2"
          ),
          color(50).bg,
          color(500).text,
          cls(s"hover:${color(100).bg.toCSS}"),
          cls(s"focus:${color(600).ring.toCSS}"),
          cls(s"focus:${color(50).ringOffset.toCSS}"),
          mods,
          aria.hidden := true,
          closeIcon
        )
      )
