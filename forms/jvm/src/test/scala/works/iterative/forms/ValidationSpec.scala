// PURPOSE: Tests for the declared validation vocabulary as a composable ValidationRule
// PURPOSE: The adapter reactive interpreters compose into field rules must match DeclaredValidation's checks

package works.iterative.forms

import zio.test.*
import works.iterative.core.{MessageCatalogue, MessageId}
import works.iterative.ui.model.forms.IdPath

object ValidationSpec extends ZIOSpecDefault:

    given MessageCatalogue = MessageCatalogue.debug

    val path = IdPath.full("demo.contact.email")

    def run(validations: List[Validation], value: String): ValidationState[String] =
        Validation.rule[Option](path, validations).apply(value).get

    def spec = suite("Validation.rule")(
        test("no declared validations accept any value") {
            assertTrue(run(Nil, "anything") == ValidationState.Valid("anything"))
        },
        test("a failing check is invalid at the field path") {
            val result = run(List(Validation.Email), "not-an-email")
            result match
                case ValidationState.Invalid(errors) =>
                    assertTrue(
                        errors.map(_._1).toList == List(path),
                        errors.map(_._2.id).toList == List(MessageId("error.field.email"))
                    )
                case other => assertTrue(other == null)
        },
        test("a passing check is valid") {
            assertTrue(
                run(List(Validation.Email), "michal@example.com") ==
                    ValidationState.Valid("michal@example.com")
            )
        },
        test("failing checks accumulate in declaration order") {
            val result = run(
                List(Validation.MinLength(5), Validation.Pattern("[0-9]+")),
                "abc"
            )
            result match
                case ValidationState.Invalid(errors) =>
                    assertTrue(errors.map(_._2.id).toList == List(
                        MessageId("error.field.minlength"),
                        MessageId("error.field.pattern")
                    ))
                case other => assertTrue(other == null)
        },
        test("Required and Rule pass through — they bind elsewhere") {
            assertTrue(
                run(List(Validation.Required, Validation.Rule("ares")), "") ==
                    ValidationState.Valid("")
            )
        }
    )
end ValidationSpec
