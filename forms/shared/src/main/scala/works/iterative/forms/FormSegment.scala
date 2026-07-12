package portaly
package forms
import works.iterative.ui.model.forms.{AbsolutePath, FormState, RelativePath}

final case class FieldType(id: String, context: Option[String] = None, disabled: Boolean = false):
    val kind: FieldKind = FieldKind.of(id)
    val hidden: Boolean = kind == FieldKind.Hidden

object FieldType:
    def apply(id: String): FieldType = FieldType(id, None)
    def apply(kind: FieldKind): FieldType = FieldType(kind.wireId, None)
    given Conversion[String, FieldType] = FieldType(_)
end FieldType

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

    /** One item of a repeated group: the path to render the template under, the raw item key and
      * type from the __items convention, and the position within the group.
      */
    case class Instance(
        path: AbsolutePath,
        item: String,
        itemType: String,
        segment: SectionSegment,
        index: Int
    )

    /** The instances of a repeated group for the current state — the one expansion every walker
      * shares. Item types without a matching template fall back to the first one; a group without
      * templates expands to nothing.
      */
    def instances(path: AbsolutePath, repeated: Repeated, state: FormState): List[Instance] =
        repeated.elems match
            case Nil => Nil
            case defaultSegment :: _ =>
                val templates = repeated.elems.map(e => e.id.last -> e).toMap
                state.itemsFor(path / repeated.id).zipWithIndex.map:
                    case ((item, itemType), index) =>
                        Instance(
                            path / repeated.id / item,
                            item,
                            itemType,
                            templates.getOrElse(itemType, defaultSegment),
                            index
                        )
end Repeated

/** What pressing a button means: Submit sends the whole form, ServerAction posts so the server
  * dispatches on the button name, ClientAction is handled by client-side code.
  */
enum ButtonIntent:
    case Submit, ServerAction, ClientAction

case class Button(id: RelativePath, intent: ButtonIntent = ButtonIntent.ServerAction)
    extends SectionSegment

case class Field(
    id: RelativePath,
    fieldType: FieldType = FieldType("string"),
    default: Option[String] = None,
    optional: Boolean = false,
    validations: List[Validation] = Nil
) extends SectionSegment

case class File(id: RelativePath, multiple: Boolean = true, optional: Boolean = false)
    extends SectionSegment

case class Date(id: RelativePath, optional: Boolean = true) extends SectionSegment

case class Display(id: RelativePath) extends SectionSegment

case class Enum(
    id: RelativePath,
    values: List[String],
    default: Option[String],
    optional: Boolean = true
) extends SectionSegment
object Enum:
    def apply(id: RelativePath)(values: String*): Enum =
        Enum(id, values.toList, default = None)

    def apply(id: RelativePath, default: Option[String])(values: String*): Enum =
        Enum(id, values.toList, default = default)

    def apply(id: RelativePath, optional: Boolean)(values: String*): Enum =
        Enum(id, values.toList, default = None, optional = optional)

    def apply(id: RelativePath, default: Option[String], optional: Boolean)(
        values: String*
    ): Enum =
        Enum(id, values.toList, default = default, optional = optional)

    def bool(id: RelativePath, default: Option[Boolean] = None, optional: Boolean = true): Enum =
        Enum(id, List("true", "false"), default = default.map(_.toString), optional = optional)
end Enum
