// PURPOSE: Each interpreter's per-kind dispatch is an explicit pinned table over the whole vocabulary
// PURPOSE: The tables are exhaustive matches — a new FieldKind fails here until every table gains a row

package works.iterative.forms.scenarios

import zio.*
import zio.test.*
import scala.xml.NodeSeq
import works.iterative.core.{Language, MessageCatalogue}
import works.iterative.forms.*
import works.iterative.forms.service.AutocompleteResolver
import works.iterative.autocomplete.service.AutocompleteService
import works.iterative.ui.model.forms.*

object FieldKindConformanceSpec extends ZIOSpecDefault:

    given MessageCatalogue = ConformanceCorpus.messages
    given Language = Language.EN

    private val builder = UIFormBuilder(LayoutResolver.grid(PartialFunction.empty))
    private val kinds = Gen.fromIterable(ConformanceCorpus.fieldKinds)

    private enum Control:
        case Input(tpe: String)
        case TextArea
        case HiddenInput

    /** What HTML control the SSR renderer produces per kind. Select-as-Field is a text input —
      * choices come from Enum, a bare select field has no values to offer.
      */
    private val ssrControl: FieldKind => Control =
        case FieldKind.Text                               => Control.Input("text")
        case FieldKind.Hidden                             => Control.HiddenInput
        case FieldKind.Date                               => Control.Input("date")
        case FieldKind.Prose                              => Control.TextArea
        case FieldKind.Select                             => Control.Input("text")
        case FieldKind.Checkbox                           => Control.Input("checkbox")
        case FieldKind.Number(_)                          => Control.Input("number")
        case FieldKind.Email                              => Control.Input("email")
        case FieldKind.Phone                              => Control.Input("tel")
        case FieldKind.Zip                                => Control.Input("text")
        case FieldKind.Country                            => Control.Input("text")
        case FieldKind.Ruian                              => Control.Input("text")
        case FieldKind.Custom(id @ ("tel" | "password"))  => Control.Input(id)
        case FieldKind.Custom(_)                          => Control.Input("text")

    /** Whether the XML renderer normalizes the decimal comma for the kind. */
    private val xmlNormalizesComma: FieldKind => Boolean =
        case FieldKind.Number(_) => true
        case FieldKind.Text | FieldKind.Hidden | FieldKind.Date | FieldKind.Prose |
            FieldKind.Select | FieldKind.Checkbox | FieldKind.Email | FieldKind.Phone |
            FieldKind.Zip | FieldKind.Country | FieldKind.Ruian | FieldKind.Custom(_) => false

    def spec = suite("FieldKind conformance")(
        test("wire ids round-trip through FieldKind.of") {
            check(kinds)(kind => assertTrue(FieldKind.of(kind.wireId) == kind))
        },
        test("UIFormBuilder passes every kind through: hidden fields aside, a labeled text field") {
            val ui = builder.buildForm(
                ConformanceCorpus.vocabularyForm,
                FormData.parse(Map.empty),
                FormValidationState.valid,
                None
            )
            check(kinds) { kind =>
                val path = IdPath.full(s"vocab.${ConformanceCorpus.fieldId(kind)}")
                val element = ui.children.collectFirst {
                    case e: UIHiddenField if e.id == path  => e
                    case e: UILabeledField if e.id == path => e
                }
                assertTrue(element.isDefined) && (element.get match
                    case UIHiddenField(_, name, _) =>
                        assertTrue(kind == FieldKind.Hidden, name == path.toHtmlName)
                    case UILabeledField(_, _, field, _) =>
                        assertTrue(field match
                            case UITextField(_, name, fieldType, _, _) =>
                                name == path.toHtmlName && fieldType == kind.wireId
                            case _ => false)
                    case _ => assertTrue(false))
            }
        },
        test("SSR renderer maps every kind to its pinned HTML control") {
            val html = UIFormHtmlRenderer(blankDisplays, FormTransport.htmx).render(
                builder.buildForm(
                    ConformanceCorpus.vocabularyForm,
                    FormData.parse(Map.empty),
                    FormValidationState.valid,
                    None
                ),
                "/conformance"
            ).render
            check(kinds) { kind =>
                val fid = ConformanceCorpus.fieldId(kind)
                val expected = ssrControl(kind) match
                    case Control.HiddenInput =>
                        s"""<input type="hidden" id="vocab-$fid" name="vocab.$fid""""
                    case Control.TextArea =>
                        s"""<textarea id="vocab-$fid" name="vocab.$fid""""
                    case Control.Input(tpe) =>
                        s"""<input type="$tpe" id="vocab-$fid" name="vocab.$fid""""
                assertTrue(html.contains(expected))
            }
        },
        test("XML renderer normalizes the decimal comma exactly for number kinds") {
            val value = "1,5"
            val state = FormData.parse(
                ConformanceCorpus.fieldKinds.map(k =>
                    s"vocab.${ConformanceCorpus.fieldId(k)}" -> Seq(value)
                ).toMap
            )
            val blankXmlDisplays = new DisplayResolver[FormState, UIO[NodeSeq]]:
                def resolve(id: IdPath, state: FormState)(using
                    MessageCatalogue,
                    Language
                ): UIO[NodeSeq] = ZIO.succeed(NodeSeq.Empty)
            val renderer = UIFormXMLRenderer(
                AutocompleteService.empty,
                AutocompleteResolver.empty,
                blankXmlDisplays,
                state
            )
            for
                xml <- renderer.render(
                    builder.buildForm(
                        ConformanceCorpus.vocabularyForm,
                        state,
                        FormValidationState.valid,
                        None
                    )
                )
                results <- check(kinds) { kind =>
                    val htmlId = s"vocab-${ConformanceCorpus.fieldId(kind)}"
                    kind match
                        case FieldKind.Hidden =>
                            // Hidden values are round-trip chrome and stay verbatim
                            val hidden = (xml \\ "hiddenField")
                                .find(_.attribute("id").exists(_.text == htmlId))
                            assertTrue(hidden.exists(_.attribute("value")
                                .exists(_.text == value)))
                        case _ =>
                            val field = (xml \\ "labeledField")
                                .find(_.attribute("id").exists(_.text == htmlId))
                            val rendered = field.map(f => (f \\ "inputValue").text.trim)
                            val expected = if xmlNormalizesComma(kind) then "1.5" else value
                            assertTrue(rendered.contains(expected))
                }
            yield results
        }
    )

    private val blankDisplays =
        new DisplayResolver[FormState, scalatags.Text.all.Frag]:
            def resolve(id: IdPath, state: FormState)(using
                MessageCatalogue,
                Language
            ): scalatags.Text.all.Frag = scalatags.Text.all.frag()
end FieldKindConformanceSpec
