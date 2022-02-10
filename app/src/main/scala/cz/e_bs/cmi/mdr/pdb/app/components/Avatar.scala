package cz.e_bs.cmi.mdr.pdb.app.components

import com.raquo.laminar.api.L.{*, given}
import com.raquo.domtypes.generic.codecs.BooleanAsTrueFalseStringCodec

// TODO: render icon or picture based on img signal
def Avatar($img: Signal[Option[String]], size: Int = 8) =
  div(
    cls := "relative",
    img(
      cls := "h-16 w-16 rounded-full",
      src := "https://images.unsplash.com/photo-1463453091185-61582044d556?ixlib=rb-=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=facearea&facepad=8&w=1024&h=1024&q=80",
      alt := ""
    ),
    span(
      cls := "absolute inset-0 shadow-inner rounded-full",
      customHtmlAttr("aria-hidden", BooleanAsTrueFalseStringCodec) := true
    )
  )
