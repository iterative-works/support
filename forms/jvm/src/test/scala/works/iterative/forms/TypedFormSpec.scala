// PURPOSE: Tests for the typed form layer — the applicative algebra erases to plain segments
// PURPOSE: and its FormCodec captures FormData as a typed value with accumulated errors

package works.iterative.forms

import zio.test.*
import zio.prelude.Validation
import works.iterative.core.{Email, MessageCatalogue, MessageId, PlainMultiLine}
import works.iterative.ui.model.forms.IdPath

object TypedFormSpec extends ZIOSpecDefault:

    case class Applicant(name: String, age: Int, note: Option[String])

    val applicant: TypedForm[Applicant] =
        TypedForm.section("applicant")(
            TypedForm.field[String]("name")
                *: TypedForm.field[Int]("age")
                *: TypedForm.field[Option[String]]("note")
                *: TypedForm.unit
        ).bimap((name, age, note) => Applicant(name, age, note))(a => (a.name, a.age, a.note))

    val (erased, codec) = applicant.form("demo", "1")

    def data(values: (String, String)*): FormData =
        FormData.parse(values.map((k, v) => k -> Seq(v)).toMap)

    def errorsOf[A](result: works.iterative.core.Validated[A]) =
        result.toEither.left.toOption.map(_.toList).getOrElse(Nil)

    def spec = suite("TypedForm")(
        test("erases to the identical hand-written Form") {
            assertTrue(erased == Form("demo", "1")(
                Section("applicant")(
                    Field("name"),
                    Field("age", FieldType("number")),
                    Field("note", optional = true)
                )
            ))
        },
        test("field types and required-ness derive from the input schema") {
            val email = TypedForm.field[Email]("contact")
            val prose = TypedForm.field[PlainMultiLine]("story")
            assertTrue(
                email.elems == List(Field("contact", FieldType("base:email"))),
                prose.elems == List(Field("story", FieldType("prose")))
            )
        },
        test("decode captures posted values as the typed value") {
            val result = codec.decode(data(
                "demo.applicant.name" -> "Jana",
                "demo.applicant.age" -> "42",
                "demo.applicant.note" -> "zn"
            ))
            assertTrue(result == Validation.succeed(Applicant("Jana", 42, Some("zn"))))
        },
        test("missing optional field decodes to None") {
            val result = codec.decode(data(
                "demo.applicant.name" -> "Jana",
                "demo.applicant.age" -> "42"
            ))
            assertTrue(result == Validation.succeed(Applicant("Jana", 42, None)))
        },
        test("decode accumulates errors across fields") {
            val errors = errorsOf(codec.decode(FormData.empty))
            assertTrue(
                errors.size == 2,
                errors.forall(_.id == MessageId("error.value.required"))
            )
        },
        test("blank input counts as missing") {
            val errors = errorsOf(codec.decode(data(
                "demo.applicant.name" -> " ",
                "demo.applicant.age" -> "42"
            )))
            assertTrue(errors.map(_.id) == List(MessageId("error.value.required")))
        },
        test("invalid values fail with the schema's message") {
            val errors = errorsOf(codec.decode(data(
                "demo.applicant.name" -> "Jana",
                "demo.applicant.age" -> "abc"
            )))
            assertTrue(errors.map(_.id) == List(MessageId("error.invalid.integer.format")))
        },
        test("encode writes values at the paths decode reads back") {
            val value = Applicant("Jana", 42, Some("zn"))
            val encoded = codec.encode(value)
            assertTrue(
                encoded.getString(IdPath.full("demo.applicant.name")) == Some("Jana"),
                codec.decode(encoded) == Validation.succeed(value),
                codec.decode(codec.encode(Applicant("Jana", 42, None)))
                    == Validation.succeed(Applicant("Jana", 42, None))
            )
        },
        test("the erased form runs the standard machinery: builder fold and declared validation") {
            given MessageCatalogue = MessageCatalogue.debug
            val ui = UIFormBuilder(LayoutResolver.grid(PartialFunction.empty))
                .buildForm(erased, FormData.empty, FormValidationState.valid, None)
            val validated = DeclaredValidation.validate(erased, FormData.empty)
            assertTrue(
                ui.id.toHtmlId == "demo",
                !validated.isValid(IdPath.full("demo.applicant.name")),
                !validated.isValid(IdPath.full("demo.applicant.age")),
                validated.isValid(IdPath.full("demo.applicant.note"))
            )
        }
    )
end TypedFormSpec
