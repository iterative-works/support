// PURPOSE: Tests for the declared validation vocabulary as a composable ValidationRule
// PURPOSE: The adapter reactive interpreters compose into field rules must match DeclaredValidation's checks

package works.iterative.forms

import zio.test.*
import works.iterative.core.{MessageCatalogue, MessageId, UserMessage}
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
        },
        test("a registry binds Rule checks by id; unbound ids keep passing") {
            val registry = new ValidationRuleRegistry:
                def check(
                    rule: Validation.Rule,
                    value: String,
                    label: => String
                ): Option[UserMessage] =
                    Option.when(rule.id == "even" && !value.toIntOption.exists(_ % 2 == 0))(
                        UserMessage("error.rule.even", label)
                    )
            assertTrue(
                Validation.check(Validation.Rule("even"), "3", "V", registry)
                    .exists(_.id == MessageId("error.rule.even")),
                Validation.check(Validation.Rule("even"), "2", "V", registry).isEmpty,
                Validation.check(Validation.Rule("unbound"), "3", "V", registry).isEmpty,
                Validation.check(Validation.Rule("even"), "3", "V").isEmpty
            )
        },
        test("Validation.rule consults the registry and accumulates its failures") {
            val registry = new ValidationRuleRegistry:
                def check(
                    rule: Validation.Rule,
                    value: String,
                    label: => String
                ): Option[UserMessage] =
                    Option.when(rule.id == "even" && !value.toIntOption.exists(_ % 2 == 0))(
                        UserMessage("error.rule.even", label)
                    )
            val result = Validation.rule[Option](
                path,
                List(Validation.MinLength(2), Validation.Rule("even")),
                registry
            ).apply("3").get
            result match
                case ValidationState.Invalid(errors) =>
                    assertTrue(errors.map(_._2.id).toList == List(
                        MessageId("error.field.minlength"),
                        MessageId("error.rule.even")
                    ))
                case other => assertTrue(other == null)
        }
    )
end ValidationSpec
