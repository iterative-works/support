// PURPOSE: Both validation paths agree on every declared Validation case — same verdict, same message
// PURPOSE: DeclaredValidation (POST loop) vs Validation.rule (reactive), and failures render as errors

package works.iterative.forms.scenarios

import zio.test.*
import works.iterative.core.{MessageCatalogue, UserMessage}
import works.iterative.forms.*
import works.iterative.ui.model.forms.IdPath

object ValidationConformanceSpec extends ZIOSpecDefault:

    given MessageCatalogue = ConformanceCorpus.messages

    private val valuePath = IdPath.full("validated.value")

    private val blankDisplays =
        new DisplayResolver[works.iterative.ui.model.forms.FormState, scalatags.Text.all.Frag]:
            def resolve(id: IdPath, state: works.iterative.ui.model.forms.FormState)(using
                MessageCatalogue,
                works.iterative.core.Language
            ): scalatags.Text.all.Frag = scalatags.Text.all.frag()
    private val samples = Gen.fromIterable(ConformanceCorpus.validationSamples)
    private val optionality = Gen.fromIterable(List(true, false))

    private def declared(
        sample: ConformanceCorpus.ValidationSample,
        optional: Boolean,
        value: Option[String]
    ): List[UserMessage] =
        val data = value.fold(Map.empty[String, Seq[String]])(v =>
            Map("validated.value" -> Seq(v))
        )
        DeclaredValidation.validate(
            ConformanceCorpus.validationForm(sample, optional),
            FormData.parse(data)
        ).errors(valuePath)
    end declared

    private def ruleMessages(
        sample: ConformanceCorpus.ValidationSample,
        value: String
    ): List[UserMessage] =
        // The renderers resolve messages under the form key; the rule adapter runs nested the
        // same way inside the live interpreter, so the comparison nests too
        given MessageCatalogue = ConformanceCorpus.messages.nested("validated")
        Validation.rule[Option](valuePath, List(sample.validation)).apply(value).get match
            case ValidationState.Valid(_)        => Nil
            case ValidationState.Invalid(errors) => errors.map(_._2).toList
            // Validation.rule never defers; surfacing the info keeps the helper total
            case ValidationState.Unknown(info) => info.map(_._2)

    def spec = suite("Validation conformance")(
        test("passing values validate in both paths") {
            check(samples, optionality) { (sample, optional) =>
                assertTrue(
                    declared(sample, optional, Some(sample.passing)).isEmpty,
                    ruleMessages(sample, sample.passing).isEmpty
                )
            }
        },
        test("failing values fail in both paths with the same message") {
            check(samples, optionality) { (sample, optional) =>
                sample.failing match
                    case None => assertTrue(true)
                    case Some(failing) =>
                        val fromDeclared = declared(sample, optional, Some(failing))
                        val fromRule = ruleMessages(sample, failing)
                        assertTrue(
                            fromDeclared == fromRule,
                            fromDeclared.map(_.id.toString) == List(sample.errorKey)
                        )
            }
        },
        test("blank is only ever the required concern — format checks never see it") {
            check(samples, Gen.fromIterable(List(None, Some(""), Some("  ")))) {
                (sample, blank) =>
                    val requiredErrors = declared(sample, optional = false, blank)
                    val optionalErrors = declared(sample, optional = true, blank)
                    val stillRequired = sample.validation == Validation.Required
                    assertTrue(
                        requiredErrors.map(_.id.toString) == List("error.field.required"),
                        // a declared Required overrides the optional flag; anything else skips blanks
                        optionalErrors.map(_.id.toString) ==
                            (if stillRequired then List("error.field.required") else Nil)
                    )
            }
        },
        test("a registered Rule fires identically in both paths") {
            val sample = ConformanceCorpus.validationSamples.find(s =>
                s.validation match
                    case Validation.Rule(_, _) => true
                    case _                     => false
            ).get
            val registry = ConformanceCorpus.ruleRegistry
            val form = ConformanceCorpus.validationForm(sample, optional = false)
            def declaredWith(value: String) =
                DeclaredValidation.validate(
                    form,
                    FormData.parse(Map("validated.value" -> Seq(value))),
                    registry
                ).errors(valuePath)
            def ruleWith(value: String) =
                given MessageCatalogue = ConformanceCorpus.messages.nested("validated")
                Validation.rule[Option](valuePath, List(sample.validation), registry)
                    .apply(value).get match
                    case ValidationState.Valid(_)        => Nil
                    case ValidationState.Invalid(errors) => errors.map(_._2).toList
                    case ValidationState.Unknown(info)   => info.map(_._2)
            assertTrue(
                declaredWith(sample.passing).isEmpty,
                ruleWith(sample.passing).isEmpty,
                declaredWith("3") == ruleWith("3"),
                declaredWith("3").map(_.id.toString) == List(sample.errorKey)
            )
        },
        test("declared failures render as field errors through builder and SSR renderer") {
            val builder = UIFormBuilder(LayoutResolver.grid(PartialFunction.empty))
            val renderer = UIFormHtmlRenderer(blankDisplays)
            // A distinctive literal from each error template, immune to argument formatting
            val fragments = Map(
                "error.field.email" -> "is not a valid e-mail",
                "error.field.pattern" -> "does not match",
                "error.field.minlength" -> "must have at least",
                "error.field.maxlength" -> "must have at most"
            )
            check(samples) { sample =>
                sample.failing match
                    case None => assertTrue(true)
                    case Some(failing) =>
                        val form = ConformanceCorpus.validationForm(sample, optional = false)
                        val state = FormData.parse(Map("validated.value" -> Seq(failing)))
                        val validation = DeclaredValidation.validate(form, state)
                        val html = renderer.render(
                            builder.buildForm(form, state, validation, None),
                            "/conformance"
                        ).render
                        assertTrue(
                            html.contains("""class="field-error""""),
                            html.contains(fragments(sample.errorKey))
                        )
            }
        }
    )
end ValidationConformanceSpec
