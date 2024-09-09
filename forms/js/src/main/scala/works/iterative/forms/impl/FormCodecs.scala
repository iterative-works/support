package portaly.forms
package impl

import zio.prelude.*
import works.iterative.ui.model.forms.IdPath

trait FormEncoder[A]:
    def toForm(a: A): FormR

trait FormDecoder[A]:
    def fromForm(data: FormR): ValidationState[A]

object FormCodecs:
    val formFormEncoder: FormEncoder[Form] = (f: Form) =>
        val props = FormR.strings("id" -> f.id.last, "version" -> f.version)
        val elems = formItemsEncoder.toForm(f.elems)
        props.under(IdPath("form.props")) <> elems.under(IdPath("form.elems"))

    val formItemsEncoder: FormEncoder[List[SectionSegment]] = (l: List[SectionSegment]) =>
        val summary =
            FormR.data((IdPath("__items")) -> l.map(i =>
                s"${i.id.last}:${discriminator(i)}"
            ))
        val items = l.map(formSectionSegmentEncoder.toForm(_)).reduceIdentity
        summary <> items

    private def discriminator(s: SectionSegment): String = s match
        case _: Section  => "section"
        case _: Field    => "field"
        case _: Enum     => "enum"
        case _: File     => "file"
        case _: Button   => "button"
        case r: Repeated => discriminator(r.elems.head)
        case s: ShowIf   => discriminator(s.elem)
        case _           => "display"

    val formSectionSegmentEncoder: FormEncoder[SectionSegment] = {
        case s: Section  => formSectionEncoder.toForm(s)
        case f: Field    => fieldEncoder.toForm(f)
        case e: Enum     => enumEncoder.toForm(e)
        case f: File     => fileEncoder.toForm(f)
        case b: Button   => buttonEncoder.toForm(b)
        case d: Display  => displayEncoder.toForm(d)
        case r: Repeated => repeatedEncoder.toForm(r)
        case s: ShowIf   => showIfEncoder.toForm(s)
        case _           => FormR.empty
    }

    val formSectionEncoder: FormEncoder[Section] = (s: Section) =>
        val section = FormR.data(IdPath("id") -> List(s.id.last))
        val elems = formItemsEncoder.toForm(s.elements)
        section.under(s.id / "section") <> elems.under(s.id / "section" / "elems")

    val fieldEncoder: FormEncoder[Field] = (f: Field) =>
        FormR.data(
            IdPath("id") -> List(f.id.last),
            IdPath("type") -> List(f.fieldType),
            IdPath("default") -> f.default.toList,
            IdPath("optional") -> List(f.optional.toString)
        ).under(f.id / "field")

    val buttonEncoder: FormEncoder[Button] = (b: Button) =>
        FormR.data(IdPath("id") -> List(b.id.last)).under(b.id / "button")

    val enumEncoder: FormEncoder[Enum] = (e: Enum) =>
        FormR.data(
            IdPath("id") -> List(e.id.last),
            IdPath("values") -> List(e.values.mkString(", ")),
            IdPath("default") -> e.default.toList
        ).under(e.id / "enum")

    val fileEncoder: FormEncoder[File] = (f: File) =>
        FormR.data(
            IdPath("id") -> List(f.id.last),
            IdPath("multiple") -> List(f.multiple.toString),
            IdPath("optional") -> List(f.optional.toString)
        ).under(f.id / "file")

    val displayEncoder: FormEncoder[Display] = (d: Display) =>
        FormR.data(IdPath("id") -> List(d.id.last)).under(d.id / "display")

    val repeatedEncoder: FormEncoder[Repeated] = (r: Repeated) =>
        formSectionSegmentEncoder.toForm(r.elems.head).add(IdPath("repeated"), "true")

    val showIfEncoder: FormEncoder[ShowIf] = (s: ShowIf) =>
        formSectionSegmentEncoder.toForm(s.elem).add(IdPath("condition"), s.condition.toString())

end FormCodecs
