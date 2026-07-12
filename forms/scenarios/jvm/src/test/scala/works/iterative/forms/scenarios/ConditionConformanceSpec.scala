// PURPOSE: Every walker that evaluates conditions agrees with the corpus visibility expectations
// PURPOSE: UIFormBuilder consults the validity view; DeclaredValidation and the encoder are always-valid

package works.iterative.forms.scenarios

import zio.test.*
import zio.json.ast.JsonCursor
import works.iterative.core.{MessageCatalogue, UserMessage}
import works.iterative.forms.*
import works.iterative.ui.model.forms.*

object ConditionConformanceSpec extends ZIOSpecDefault:

    given MessageCatalogue = ConformanceCorpus.messages

    private val builder = UIFormBuilder(LayoutResolver.grid(PartialFunction.empty))
    private val markerPath = IdPath.full("conditioned.gated.marker")

    private def state(sample: ConformanceCorpus.ConditionSample) =
        FormData.parse(ConformanceCorpus.conditionData(sample))

    private def validity(sample: ConformanceCorpus.ConditionSample): FormValidationState =
        MapFormValidationState(
            sample.invalid.map(s =>
                IdPath.full(s"conditioned.$s") -> List(UserMessage("error.conformance"))
            ).toMap
        )

    private def containsMarker(elements: Seq[UIFormElement]): Boolean =
        elements.exists:
            case UILabeledField(id, _, _, _)            => id == markerPath
            case UIFormSection(_, _, _, children, _, _) => containsMarker(children)
            case UIGrid(rows) => containsMarker(rows.flatten.flatMap(_.children))
            case UIFlexRow(children)                => containsMarker(children)
            case UIRepeatedGroup(_, _, _, _, rows, _) =>
                containsMarker(rows.flatMap(_.children))
            case _ => false

    private val samples = Gen.fromIterable(ConformanceCorpus.conditionSamples)

    def spec = suite("Condition conformance")(
        test("UIFormBuilder includes the gated content per its validity view") {
            check(samples) { sample =>
                val ui = builder.buildForm(
                    ConformanceCorpus.conditionForm(sample),
                    state(sample),
                    validity(sample),
                    None
                )
                assertTrue(containsMarker(ui.children) == sample.visible)
            }
        },
        test("DeclaredValidation validates the gated content only when visible") {
            check(samples) { sample =>
                // The marker is required and left blank, so a required error marks visibility
                val form = Form("conditioned", "1")(
                    Section("flags")(
                        Field("gate", optional = true),
                        Field("other", optional = true)
                    ),
                    ShowIf(sample.condition, Section("gated")(Field("marker")))
                )
                val blankMarker = FormData.parse(
                    ConformanceCorpus.conditionData(sample) - "conditioned.gated.marker"
                )
                val result = DeclaredValidation.validate(form, blankMarker)
                assertTrue(
                    result.errors(markerPath).nonEmpty == sample.visibleWhenAlwaysValid
                )
            }
        },
        test("FormRJsonEncoder emits the gated content only when visible") {
            check(samples) { sample =>
                val json = FormRJsonEncoder().toJsonAST(
                    ConformanceCorpus.conditionForm(sample),
                    state(sample)
                )
                val gated = json.get(
                    JsonCursor.field("data") >>> JsonCursor.isObject >>> JsonCursor.field("gated")
                )
                assertTrue(gated.isRight == sample.visibleWhenAlwaysValid)
            }
        }
    )
end ConditionConformanceSpec
