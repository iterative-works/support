package portaly.forms

import com.raquo.laminar.api.L.*
import works.iterative.ui.model.forms.UIFormSection
import works.iterative.ui.model.forms.FormState

trait UIFormReadOnlyHooks:
    def amendSection(
        section: UIFormSection,
        data: FormState,
        sectionElement: HtmlElement
    ): HtmlElement
end UIFormReadOnlyHooks

object UIFormReadOnlyHooks:
    val empty: UIFormReadOnlyHooks = new UIFormReadOnlyHooks:
        override def amendSection(
            section: UIFormSection,
            data: FormState,
            sectionElement: HtmlElement
        ): HtmlElement = sectionElement
end UIFormReadOnlyHooks
