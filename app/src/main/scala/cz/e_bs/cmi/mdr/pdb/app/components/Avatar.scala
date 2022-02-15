package cz.e_bs.cmi.mdr.pdb.app.components

import CustomAttrs.ariaHidden
import com.raquo.laminar.api.L.{*, given}
import com.raquo.domtypes.generic.codecs.BooleanAsTrueFalseStringCodec

// TODO: render icon or picture based on img signal
class Avatar($avatarImg: Signal[Option[String]]):
  inline def avatarPlaceholder(size: Int): HtmlElement =
    div(
      cls := s"rounded-full text-indigo-200 bg-indigo-500 h-${size} w-${size} flex items-center justify-center",
      Icons.outline.user(size - 2)
    )

  inline def avatarImage(size: Int): Signal[HtmlElement] =
    $avatarImg.split(_ => ())((_, _, $url) =>
      img(
        cls := s"w-$size h-$size rounded-full",
        src <-- $url,
        alt := ""
      )
    ).map(_.getOrElse(avatarPlaceholder(size)))

  def avatar(size: Int): HtmlElement =
    div(
      cls := "relative",
      child <-- avatarImage(size),
      span(
        cls := "absolute inset-0 shadow-inner rounded-full",
        ariaHidden := true
      )
    )
