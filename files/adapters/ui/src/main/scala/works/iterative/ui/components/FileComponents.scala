package works.iterative.ui.components

import com.raquo.laminar.api.L.*
import works.iterative.core.FileRef

trait FileComponents:
    def baseHref: String
    // I hate using ComponentContext all around, we need to drop it
    def renderFileRefs(
        ref: Seq[FileRef],
        metadata: Option[(FileRef, Int) => Map[String, String]] = None,
        rolledUp: Boolean = false
    )(using
        ComponentContext[?]
    ): HtmlElement

    def renderFileLink(ref: FileRef): HtmlElement
end FileComponents
