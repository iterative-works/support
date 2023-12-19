package works.iterative.ui.components.laminar

import com.raquo.laminar.api.L.*
import com.raquo.laminar.codecs.{StringAsIsCodec, BooleanAsTrueFalseStringCodec}

object CustomAttrs:
    // Made a pull request to add aria-current to scala-dom-types, remove after
    val ariaCurrent = htmlAttr("aria-current", StringAsIsCodec)
    val ariaHidden = htmlAttr("aria-hidden", BooleanAsTrueFalseStringCodec)

    val datetime = htmlAttr("datetime", StringAsIsCodec)

    object svg:
        import com.raquo.laminar.api.L.svg.*
        val ariaHidden =
            svgAttr("aria-hidden", BooleanAsTrueFalseStringCodec, None)
    end svg
end CustomAttrs
