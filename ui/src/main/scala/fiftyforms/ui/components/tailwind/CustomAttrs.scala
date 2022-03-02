package fiftyforms.ui.components.tailwind

import com.raquo.laminar.api.L.{*, given}
import com.raquo.domtypes.generic.codecs.StringAsIsCodec
import com.raquo.domtypes.generic.codecs.BooleanAsTrueFalseStringCodec

object CustomAttrs {
  // Made a pull request to add aria-current to scala-dom-types, remove after
  val ariaCurrent = customHtmlAttr("aria-current", StringAsIsCodec)
  val ariaHidden = customHtmlAttr("aria-hidden", BooleanAsTrueFalseStringCodec)

  val datetime = customHtmlAttr("datetime", StringAsIsCodec)

  object svg {
    import com.raquo.laminar.api.L.svg.{*, given}
    val ariaHidden =
      customSvgAttr("aria-hidden", BooleanAsTrueFalseStringCodec)
  }
}
