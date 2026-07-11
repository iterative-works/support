// PURPOSE: Tests for Required-only validation of submitted data against the form declaration
// PURPOSE: Drives the ssr-html-interpreter POST loop; seed of the declared validation vocabulary

package portaly.forms

import zio.test.*
import works.iterative.core.{MessageCatalogue, MessageId}
import works.iterative.ui.model.forms.IdPath
import portaly.forms.impl.FormR

object RequiredValidationSpec extends ZIOSpecDefault:

    given MessageCatalogue = MessageCatalogue.debug

    val requiredMessage = MessageId("error.field.required")

    def spec = suite("RequiredValidation")(
        test("blank required field is invalid, filled one is valid") {
            val form = Form("demo", "1")(Section("contact")(Field("name")))
            val path = IdPath.full("demo.contact.name")
            val missing = RequiredValidation.validate(form, FormR.empty)
            val blank = RequiredValidation.validate(
                form,
                FormR.strings("demo.contact.name" -> " ")
            )
            val filled = RequiredValidation.validate(
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
            val result = RequiredValidation.validate(form, FormR.empty)
            assertTrue(
                result.isValid(IdPath.full("demo.contact.email")),
                result.isValid(IdPath.full("demo.contact.token"))
            )
        },
        test("declared default satisfies a required field") {
            val form = Form("demo", "1")(
                Section("contact")(Field("city", default = Some("Brno")))
            )
            val result = RequiredValidation.validate(form, FormR.empty)
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
            val hidden = RequiredValidation.validate(
                form,
                FormR.strings("demo.main.kind" -> "ordinary")
            )
            val shown = RequiredValidation.validate(
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
            val result = RequiredValidation.validate(form, state)
            assertTrue(
                result.isValid(IdPath.full("demo.items.first.row.qty")),
                !result.isValid(IdPath.full("demo.items.second.row.qty"))
            )
        },
        test("required repeated with no items is invalid, optional one is valid") {
            def repeated(optional: Boolean) = Form("demo", "1")(
                Repeated("items", optional = optional)(Section("row")(Field("qty")))
            )
            val required = RequiredValidation.validate(repeated(false), FormR.empty)
            val optional = RequiredValidation.validate(repeated(true), FormR.empty)
            assertTrue(
                !required.isValid(IdPath.full("demo.items")),
                optional.isValid(IdPath.full("demo.items"))
            )
        },
        test("required file field without files is invalid") {
            val form = Form("demo", "1")(Section("docs")(File("attachment")))
            val result = RequiredValidation.validate(form, FormR.empty)
            assertTrue(!result.isValid(IdPath.full("demo.docs.attachment")))
        }
    )
end RequiredValidationSpec
