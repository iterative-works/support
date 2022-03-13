package works.iterative.ui.components.tailwind

import CustomAttrs.ariaHidden
import com.raquo.laminar.api.L.{*, given}
import com.raquo.domtypes.generic.codecs.BooleanAsTrueFalseStringCodec
import works.iterative.ui.components.tailwind.Macros

// TODO: render icon or picture based on img signal
class Avatar($avatarImg: Signal[Option[String]]):
  inline def avatarPlaceholder(size: Int): HtmlElement =
    div(
      cls := s"rounded-full text-indigo-200 bg-indigo-500 flex items-center justify-center",
      cls := Macros.size(size),
      Icons.outline.user(size - 2)
    )

  inline def avatarImage(size: Int): Signal[HtmlElement] =
    $avatarImg.split(_ => ())((_, _, $url) =>
      img(
        cls := s"rounded-full",
        cls := Macros.size(size),
        src <-- $url,
        alt := ""
      )
    ).map(_.getOrElse(avatarPlaceholder(size)))

  inline def avatar(size: Int): HtmlElement =
    div(
      cls := "relative",
      child <-- avatarImage(size),
      span(
        cls := "absolute inset-0 shadow-inner rounded-full",
        ariaHidden := true
      )
    )
