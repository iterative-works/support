package portaly.forms
package impl

import works.iterative.ui.model.forms.RelativePath

trait LiveFormHooks:
    def aroundSection(sectionId: RelativePath)(sectionPart: Part): Part
end LiveFormHooks

object LiveFormHooks:
    def empty: LiveFormHooks = new LiveFormHooks:
        override def aroundSection(sectionId: RelativePath)(sectionPart: Part): Part = sectionPart
end LiveFormHooks
