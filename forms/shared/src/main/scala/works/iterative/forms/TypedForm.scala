// PURPOSE: Typed form declarations — an applicative algebra emitting plain FormSegments
// PURPOSE: plus a FormCodec capturing FormData as a typed value with accumulated errors

package works.iterative.forms

import works.iterative.core.Validated
import works.iterative.ui.components.laminar.forms.InputSchema
import works.iterative.ui.model.forms.{AbsolutePath, IdPath, RelativePath}
import zio.prelude.Validation

/** Typed capture of a form's data, bound to the form's root path: decode accumulates errors across
  * fields, encode produces the FormData an interpreter renders from.
  */
final case class FormCodec[A](decode: FormData => Validated[A], encode: A => FormData)

/** A form declaration paired with its typed codec. The declaration erases to plain segments, so
  * interpreters never see the typed layer and it cannot fork the core.
  */
final case class TypedForm[A](
    elems: List[SectionSegment],
    decodeAt: (AbsolutePath, FormData) => Validated[A],
    encodeAt: (AbsolutePath, A) => FormData
):
    def zip[B <: Tuple](that: TypedForm[B]): TypedForm[A *: B] =
        TypedForm(
            elems ++ that.elems,
            (path, data) =>
                Validation.validateWith(decodeAt(path, data), that.decodeAt(path, data))(_ *: _),
            (path, value) => encodeAt(path, value.head).combineWith(that.encodeAt(path, value.tail))
        )

    def bimap[B](f: A => B)(g: B => A): TypedForm[B] =
        TypedForm(
            elems,
            (path, data) => decodeAt(path, data).map(f),
            (path, value) => encodeAt(path, g(value))
        )

    /** Erases to the plain Form interpreters consume, with the codec bound to the form's root. */
    def form(id: RelativePath, version: String): (Form, FormCodec[A]) =
        val root = IdPath.Root / id
        (Form(id, version, elems), FormCodec(decodeAt(root, _), encodeAt(root, _)))
end TypedForm

object TypedForm:
    extension [A](t: TypedForm[A])
        def *:[B <: Tuple](that: TypedForm[B]): TypedForm[A *: B] = t.zip(that)

    val unit: TypedForm[EmptyTuple] =
        TypedForm(Nil, (_, _) => Validation.succeed(EmptyTuple), (_, _) => FormData.empty)

    /** A field whose type, required-ness and codec derive from the input schema: a present
      * non-blank value decodes through the schema, a missing or blank one is a required error for
      * required schemas and a success for optional ones.
      */
    def field[A](id: RelativePath)(using schema: InputSchema[A]): TypedForm[A] =
        TypedForm(
            List(Field(id, fieldTypeOf(schema.inputType), optional = !schema.required)),
            (path, data) =>
                data.getString(path / id).filterNot(_.isBlank) match
                    case Some(value) => schema.decode(value)
                    case None        => schema.decodeOptional((path / id).serialize)(None)
            ,
            (path, value) => FormData.empty.set(path / id, List(schema.encode(value)))
        )

    def section[A](id: RelativePath, sectionType: String = "any")(
        content: TypedForm[A]
    ): TypedForm[A] =
        TypedForm(
            List(Section(id, content.elems, sectionType)),
            (path, data) => content.decodeAt(path / id, data),
            (path, value) => content.encodeAt(path / id, value)
        )

    private def fieldTypeOf(inputType: InputSchema.InputType): FieldType = inputType match
        case InputSchema.InputType.Input(tpe) => FieldType(FieldKind.of(tpe))
        case InputSchema.InputType.Textarea   => FieldType(FieldKind.Prose)
end TypedForm
