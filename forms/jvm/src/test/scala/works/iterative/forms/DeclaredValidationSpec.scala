// PURPOSE: Tests for validation of submitted data against the form declaration
// PURPOSE: Required-ness from the optional flag plus the declared validation vocabulary; drives the SSR POST loop

package portaly.forms

import zio.test.*
import works.iterative.core.{MessageCatalogue, MessageId}
import works.iterative.ui.model.forms.IdPath
import portaly.forms.impl.FormR

object DeclaredValidationSpec extends ZIOSpecDefault:

    given MessageCatalogue = MessageCatalogue.debug

    val requiredMessage = MessageId("error.field.required")

    def spec = suite("DeclaredValidation")(
        test("blank required field is invalid, filled one is valid") {
            val form = Form("demo", "1")(Section("contact")(Field("name")))
            val path = IdPath.full("demo.contact.name")
            val missing = DeclaredValidation.validate(form, FormR.empty)
            val blank = DeclaredValidation.validate(
                form,
                FormR.strings("demo.contact.name" -> " ")
            )
            val filled = DeclaredValidation.validate(
                form,
                FormR.strings("demo.contact.name" -> "John")
            )
            assertTrue(
                !missing.isValid(path),
                missing.errors(path).map(_.id) == List(requiredMessage),
                missing.hasErrors,
                !blank.isValid(path),
                filled.isValid(path),
                filled.errors(path).isEmpty,
                !filled.hasErrors
            )
        },
        test("optional and hidden fields are not required") {
            val form = Form("demo", "1")(
                Section("contact")(
                    Field("email", FieldType("email"), optional = true),
                    Field("token", FieldType("hidden"))
                )
            )
            val result = DeclaredValidation.validate(form, FormR.empty)
            assertTrue(
                result.isValid(IdPath.full("demo.contact.email")),
                result.isValid(IdPath.full("demo.contact.token"))
            )
        },
        test("declared default satisfies a required field") {
            val form = Form("demo", "1")(
                Section("contact")(Field("city", default = Some("Brno")))
            )
            val result = DeclaredValidation.validate(form, FormR.empty)
            assertTrue(result.isValid(IdPath.full("demo.contact.city")))
        },
        test("fields behind an unmatched ShowIf are not validated") {
            def form = Form("demo", "1")(
                Section("main")(Field("kind")),
                ShowIf(
                    Condition.IsEqual(".demo.main.kind", "special"),
                    Section("extra")(Field("detail"))
                )
            )
            val hidden = DeclaredValidation.validate(
                form,
                FormR.strings("demo.main.kind" -> "ordinary")
            )
            val shown = DeclaredValidation.validate(
                form,
                FormR.strings("demo.main.kind" -> "special")
            )
            assertTrue(
                hidden.isValid(IdPath.full("demo.extra.detail")),
                !shown.isValid(IdPath.full("demo.extra.detail"))
            )
        },
        test("repeated items validate per expanded index") {
            val form = Form("demo", "1")(
                Repeated("items", optional = true)(Section("row")(Field("qty")))
            )
            val state = FormR(Map(
                IdPath("demo.items.__items") -> List("first:row", "second:row"),
                IdPath("demo.items.first.row.qty") -> List("1")
            ))
            val result = DeclaredValidation.validate(form, state)
            assertTrue(
                result.isValid(IdPath.full("demo.items.first.row.qty")),
                !result.isValid(IdPath.full("demo.items.second.row.qty"))
            )
        },
        test("required repeated with no items is invalid, optional one is valid") {
            def repeated(optional: Boolean) = Form("demo", "1")(
                Repeated("items", optional = optional)(Section("row")(Field("qty")))
            )
            val required = DeclaredValidation.validate(repeated(false), FormR.empty)
            val optional = DeclaredValidation.validate(repeated(true), FormR.empty)
            assertTrue(
                !required.isValid(IdPath.full("demo.items")),
                optional.isValid(IdPath.full("demo.items"))
            )
        },
        test("error labels resolve through the form-scoped catalogue like the renderers") {
            // Repeated rows have dynamic path segments (item keys); the label must fall
            // back through the suffix chain under the form prefix: inquiry.row.qty.label
            val catalogue = works.iterative.core.service.impl.InMemoryMessageCatalogue(
                works.iterative.core.Language.EN,
                Map("inquiry.row.qty.label" -> "Quantity")
            )
            val form = Form("inquiry", "1")(
                Repeated("items", optional = true)(Section("row")(Field("qty")))
            )
            val state = FormR(Map(IdPath("inquiry.items.__items") -> List("i1:row")))
            val result = DeclaredValidation.validate(form, state)(using catalogue)
            val args = result.errors(IdPath.full("inquiry.items.i1.row.qty")).head.args
            assertTrue(args == Seq("Quantity"))
        },
        test("required file field without files is invalid") {
            val form = Form("demo", "1")(Section("docs")(File("attachment")))
            val result = DeclaredValidation.validate(form, FormR.empty)
            assertTrue(!result.isValid(IdPath.full("demo.docs.attachment")))
        },
        test("declared Email rejects malformed values and skips blank optional ones") {
            def form = Form("demo", "1")(Section("contact")(
                Field("email", optional = true, validations = List(Validation.Email))
            ))
            val path = IdPath.full("demo.contact.email")
            def check(value: String) = DeclaredValidation.validate(
                form,
                FormR.strings("demo.contact.email" -> value)
            )
            assertTrue(
                !check("not-an-email").isValid(path),
                check("not-an-email").errors(path).map(_.id) ==
                    List(MessageId("error.field.email")),
                check("jane@example.com").isValid(path),
                check(" ").isValid(path)
            )
        },
        test("Pattern, MinLength and MaxLength check the effective value") {
            def form(validation: Validation, default: Option[String] = None) =
                Form("demo", "1")(Section("main")(
                    Field("v", default = default, validations = List(validation))
                ))
            val path = IdPath.full("demo.main.v")
            def check(validation: Validation, value: String) =
                DeclaredValidation.validate(
                    form(validation),
                    FormR.strings("demo.main.v" -> value)
                ).isValid(path)
            val defaultChecked = DeclaredValidation.validate(
                form(Validation.MaxLength(2), default = Some("abc")),
                FormR.empty
            )
            assertTrue(
                !check(Validation.Pattern("[0-9]+"), "abc"),
                check(Validation.Pattern("[0-9]+"), "123"),
                !check(Validation.MinLength(3), "ab"),
                check(Validation.MinLength(3), "abc"),
                !check(Validation.MaxLength(2), "abc"),
                check(Validation.MaxLength(2), "ab"),
                !defaultChecked.isValid(path)
            )
        },
        test("declared Required makes an optional-flagged field required") {
            val form = Form("demo", "1")(Section("main")(
                Field("v", optional = true, validations = List(Validation.Required))
            ))
            val result = DeclaredValidation.validate(form, FormR.empty)
            assertTrue(!result.isValid(IdPath.full("demo.main.v")))
        },
        test("Rule validations are not evaluated by the declaration walk") {
            // Async and client rules bind at the edges behind a registry
            val form = Form("demo", "1")(Section("main")(
                Field("v", validations = List(Validation.Rule("ares")))
            ))
            val result = DeclaredValidation.validate(
                form,
                FormR.strings("demo.main.v" -> "whatever")
            )
            assertTrue(result.isValid(IdPath.full("demo.main.v")))
        }
    )
end DeclaredValidationSpec
