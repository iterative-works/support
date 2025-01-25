package portaly
package forms
import works.iterative.ui.model.forms.RelativePath

final case class FieldType(id: String, context: Option[String] = None, disabled: Boolean = false):
    val hidden: Boolean = id == "hidden"

object FieldType:
    def apply(id: String): FieldType = FieldType(id, None)
    given Conversion[String, FieldType] = FieldType(_)

sealed trait FormSegment
sealed trait SectionSegment extends FormSegment:
    def id: RelativePath

case class Section(
    id: RelativePath,
    elements: List[SectionSegment],
    sectionType: String
) extends SectionSegment

object Section:
    def apply(id: RelativePath, sectionType: String = "any")(
        elems: SectionSegment*
    ): Section =
        Section(id, elems.toList, sectionType)
end Section

case class Form(id: RelativePath, version: String, elems: List[SectionSegment]) extends FormSegment:
    def idString: String = id.serialize

object Form:
    def apply(id: RelativePath, version: String)(elems: SectionSegment*): Form =
        Form(id, version, elems.toList)

// These are definitions of how many times a section can be repeated
// Every element is there exactly once, unless it is wrapped in a Cardinality
// TODO: we will need to cover all the cases, basically stating lower and upper limit
// Also, there might be a need to bind the counts to other form elements
sealed trait Cardinality extends SectionSegment

// repeat 0 or 1 times, based on condition
// TODO: How about Maybe as a name?
case class ShowIf(condition: Condition, elem: SectionSegment)
    extends Cardinality:
    override val id: RelativePath = elem.id

// repeat 1 or more times
case class Repeated(
    id: RelativePath,
    default: Option[(String, String)],
    // At least one element needed unless optional
    optional: Boolean,
    elems: List[SectionSegment]
) extends Cardinality

object Repeated:
    def apply(
        id: RelativePath,
        default: Option[(String, String)] = None,
        optional: Boolean = true
    )(elems: SectionSegment*): Repeated =
        Repeated(id, default, optional, elems.toList)
end Repeated

case class Button(id: RelativePath) extends SectionSegment

case class Field(
    id: RelativePath,
    fieldType: FieldType = FieldType("string"),
    default: Option[String] = None,
    optional: Boolean = false
) extends SectionSegment

case class File(id: RelativePath, multiple: Boolean = true, optional: Boolean = false)
    extends SectionSegment

case class Date(id: RelativePath) extends SectionSegment

case class Display(id: RelativePath) extends SectionSegment

case class Enum(
    id: RelativePath,
    values: List[String],
    default: Option[String]
) extends SectionSegment
object Enum:
    def apply(id: RelativePath, default: Option[String] = None)(values: String*): Enum =
        Enum(id, values.toList, default = default)

    def bool(id: RelativePath, default: Option[Boolean] = None): Enum =
        Enum(id, List("true", "false"), default = default.map(_.toString))

    def yesno(id: RelativePath, default: Option[Boolean] = None): Enum =
        Enum(id, List("ano", "ne"), default = default.map(_.toString))
end Enum
