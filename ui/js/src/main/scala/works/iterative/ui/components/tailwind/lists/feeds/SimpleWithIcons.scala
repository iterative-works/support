package works.iterative
package ui.components.tailwind.lists.feeds

import com.raquo.laminar.api.L.{*, given}
import java.time.Instant
import works.iterative.ui.components.tailwind.TimeUtils
import java.time.temporal.TemporalAccessor
import java.text.DateFormat
import com.raquo.laminar.codecs.StringAsIsCodec
import java.time.format.DateTimeFormatter
import java.time.ZoneId

object SimpleWithIcons:
  def simpleDate(i: TemporalAccessor): HtmlElement =
    timeTag(
      htmlAttr(
        "datetime",
        StringAsIsCodec
      ) := DateTimeFormatter.ISO_LOCAL_DATE
        .withZone(ZoneId.of("CET"))
        .format(i),
      TimeUtils.formatDate(i)
    )

  def item(
      icon: SvgElement,
      text: HtmlElement,
      date: HtmlElement,
      last: Boolean
  ): HtmlElement =
    val lastDivider: Modifier[HtmlElement] =
      span(
        cls(
          "absolute top-4 left-4 -ml-px h-full w-0.5 bg-gray-200"
        ),
        aria.hidden := true
      )
    li(
      div(
        cls("relative pb-8"),
        if !last then lastDivider else emptyMod,
        div(
          cls("relative flex space-x-3"),
          div(
            span(
              cls(
                "h-8 w-8 rounded-full bg-gray-400 flex items-center justify-center ring-8 ring-white"
              ),
              icon
            )
          ),
          div(
            cls("min-w-0 flex-1 pt-1.5 flex justify-between space-x-4"),
            div(p(cls("text-sm text-gray-500")), text),
            div(cls("text-right text-sm whitespace-nowrap text-gray-500"), date)
          )
        )
      )
    )

  def apply(items: Seq[HtmlElement]): HtmlElement =
    div(cls("flow-root"), ul(role("list"), cls("-mb-8"), items))
