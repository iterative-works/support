package portaly
package forms
import works.iterative.ui.model.forms.IdPath

trait LayoutResolver:
    def resolve(sectionId: IdPath, elems: List[SectionSegment]): Layout

object LayoutResolver:
    def grid(
        extra: PartialFunction[IdPath, List[SectionSegment] => Layout]
    ): LayoutResolver =
        new LayoutResolver:
            override def resolve(
                sectionId: IdPath,
                elems: List[SectionSegment]
            ): Layout =
                if extra.isDefinedAt(sectionId) then extra(sectionId)(elems)
                else Grid(elems.map(List(_)))
            end resolve
end LayoutResolver
