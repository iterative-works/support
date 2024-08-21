package works.iterative.ui.components.laminar.forms

import works.iterative.ui.components.ComponentContext
import com.raquo.laminar.api.L.*

final case class SectionDescriptor(
    title: String,
    subtitle: Option[String],
    extraContent: HtmlMod
):
    def extraContent(mod: HtmlMod): SectionDescriptor = copy(extraContent = mod)
end SectionDescriptor

object SectionDescriptor:
    def apply(id: String)(using ctx: ComponentContext[?]): SectionDescriptor =
        SectionDescriptor(ctx.messages(id), ctx.messages.get(id + ".subtitle"), emptyMod)
end SectionDescriptor
