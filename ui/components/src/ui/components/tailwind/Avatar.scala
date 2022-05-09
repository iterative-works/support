package works.iterative.ui.components.tailwind

import CustomAttrs.ariaHidden
import com.raquo.laminar.api.L.{*, given}
import com.raquo.domtypes.generic.codecs.BooleanAsTrueFalseStringCodec

// TODO: macros for size
class Avatar($avatarImg: Signal[Option[String]]):
  inline def avatarPlaceholder(
      extraClasses: String,
      iconClasses: String
  ): HtmlElement =
    div(
      cls := s"rounded-full text-indigo-200 bg-indigo-500 flex items-center justify-center",
      cls := extraClasses,
      Icons.outline.user(iconClasses)
    )

  inline def avatarImage(
      extraClasses: String,
      iconClasses: String
  ): Signal[HtmlElement] =
    $avatarImg.split(_ => ())((_, _, $url) =>
      img(
        cls := s"rounded-full",
        cls := extraClasses,
        src <-- $url,
        alt := ""
      )
    ).map(_.getOrElse(avatarPlaceholder(extraClasses, iconClasses)))

  inline def avatar(extraClasses: String, iconClasses: String): HtmlElement =
    div(
      cls := "relative",
      child <-- avatarImage(extraClasses, iconClasses),
      span(
        cls := "absolute inset-0 shadow-inner rounded-full",
        ariaHidden := true
      )
    )
