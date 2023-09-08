package works.iterative.ui.components.laminar

import com.raquo.laminar.api.L.*
import java.time.LocalDate
import works.iterative.core.PlainMultiLine
import java.time.Instant
import works.iterative.ui.TimeUtils
import works.iterative.core.UserMessage
import works.iterative.ui.components.ComponentContext
import LaminarExtensions.*

trait HtmlRenderable[A]:
  def toHtml(a: A): Modifier[HtmlElement]
  extension (a: A) def render: Modifier[HtmlElement] = toHtml(a)

object HtmlRenderable:
  given elementValue: HtmlRenderable[HtmlElement] with
    def toHtml(a: HtmlElement): Modifier[HtmlElement] = a

  given stringValue: HtmlRenderable[String] with
    def toHtml(v: String): Modifier[HtmlElement] =
      com.raquo.laminar.nodes.TextNode(v)

  given dateValue: HtmlRenderable[LocalDate] with
    def toHtml(v: LocalDate): Modifier[HtmlElement] =
      timeTag(
        CustomAttrs.datetime(TimeUtils.formatHtmlDate(v)),
        TimeUtils.formatDate(v)
      )

  given instantValue: HtmlRenderable[Instant] with
    def toHtml(v: Instant): Modifier[HtmlElement] =
      timeTag(
        CustomAttrs.datetime(TimeUtils.formatHtmlDateTime(v)),
        TimeUtils.formatDateTime(v)
      )

  given plainMultiLineValue: HtmlRenderable[PlainMultiLine] with
    def toHtml(v: PlainMultiLine): Modifier[HtmlElement] =
      p(cls("whitespace-pre-wrap"), v.toString)

  given optionRenderable[A](using
      r: HtmlRenderable[A]
  ): HtmlRenderable[Option[A]] with
    def toHtml(v: Option[A]): Modifier[HtmlElement] =
      v.map(r.toHtml)

  given signalRenderable[A](using
      r: HtmlRenderable[A]
  ): HtmlRenderable[Signal[A]] with
    def toHtml(v: Signal[A]): Modifier[HtmlElement] =
      child <-- v.map(a => div(r.toHtml(a)))

  given iterableRenderable[A, T[X] <: Iterable[X]](using
      inner: HtmlRenderable[A]
  ): HtmlRenderable[T[A]] with
    def toHtml(iterable: T[A]): HtmlMod =
      ul(
        iterable
          .map(inner.toHtml)
          .toSeq
          .map(i =>
            li(
              cls(
                "inline-block first:ml-0 ml-2 after:content-[','] last:after:content-none"
              ),
              i
            )
          )*
      )

  given userMessageRenderable(using
      ComponentContext[?]
  ): HtmlRenderable[UserMessage] with
    def toHtml(msg: UserMessage): HtmlMod =
      msg.asElement
