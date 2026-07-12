// PURPOSE: The proof form's POST-loop state machine, shared by every SSR transport scenario
// PURPOSE: Turns raw posted fields into the next render state or the submitted payload

package works.iterative.forms.scenarios

import scalatags.Text.all.*
import works.iterative.core.{Language, MessageCatalogue}
import works.iterative.forms.*
import works.iterative.ui.model.forms.{FormState, IdPath}

/** The transport-agnostic half of the proof form scenarios: what a POSTed field set means —
  * add/remove a repeated row, a re-render, a rejected or an accepted submission. Each transport
  * scenario renders the outcome its own way (HTML fragment swap, patch-elements SSE, full page).
  */
object InquiryFormLoop:

    given MessageCatalogue = InquiryProofForm.messages

    val formDeclaration: Form = InquiryProofForm.declaration

    val initialState: FormData = FormData.parse(InquiryProofForm.initialItems)

    private val itemsPath = IdPath.full("inquiry.items")
    private val addItemKey = "inquiry.controls.addItem"
    private val removeItemKey = "inquiry\\.items\\.([^.]+)\\.row\\.remove".r

    enum Outcome:
        case Render(state: FormData, validation: FormValidationState)
        case Submitted(data: FormData)

    def transition(raw: Map[String, Seq[String]]): Outcome =
        val data = FormData.parse(raw)
        val removed = raw.keys.collectFirst { case removeItemKey(key) => key }
        if raw.contains(addItemKey) then
            val nextIndex = data.itemsFor(itemsPath)
                .flatMap((key, _) => key.stripPrefix("i").toIntOption)
                .maxOption.getOrElse(0) + 1
            Outcome.Render(
                data.add(itemsPath / "__items", s"i$nextIndex:row"),
                FormValidationState.valid
            )
        else
            removed match
                case Some(key) =>
                    val remaining = data.itemsFor(itemsPath)
                        .filterNot(_._1 == key)
                        .map((k, t) => s"$k:$t")
                    val cleaned = data
                        .filterKeys(!_.serialize.startsWith(s"inquiry.items.$key."))
                        .set(itemsPath / "__items", remaining)
                    Outcome.Render(cleaned, FormValidationState.valid)
                case None if raw.contains("__submit") =>
                    val validation = DeclaredValidation.validate(formDeclaration, data)
                    if validation.hasErrors then Outcome.Render(data, validation)
                    else Outcome.Submitted(data)
                case None =>
                    Outcome.Render(data, FormValidationState.valid)
        end if
    end transition

    val displayResolver: DisplayResolver[FormState, Frag] =
        new DisplayResolver[FormState, Frag]:
            def resolve(path: IdPath, state: FormState)(using MessageCatalogue, Language): Frag =
                val kind = state.getString(IdPath.full("inquiry.request.kind")).getOrElse("quote")
                val items = state.itemsFor(itemsPath).size
                p(s"You are requesting a $kind with $items item(s).")

    def receivedFrag(data: FormData): Frag =
        import zio.json.*
        val dump = data.data.map((path, values) =>
            path.toHtmlName -> values.map {
                case FieldValue.Text(value) => value
                case FieldValue.File(ref)   => ref.name
            }
        )
        frag(
            h1("Inquiry received"),
            p("Submitted data:"),
            pre(code(dump.toJson))
        )
    end receivedFrag
end InquiryFormLoop
