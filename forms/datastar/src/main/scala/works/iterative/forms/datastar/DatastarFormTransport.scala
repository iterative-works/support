// PURPOSE: Datastar transport for SSR forms — data-on wiring that posts the form as form data
// PURPOSE: so the server can answer with datastar-patch-elements morphs of the form element

package works.iterative.forms.datastar

import scalatags.Text.all.*
import works.iterative.forms.FormTransport
import works.iterative.scalatags.datastar.Datastar.*

/** Wires the rendered form to Datastar: every submission and committed-choice change posts the
  * form's fields (`contentType: 'form'`) and expects the response to patch the form element back.
  *
  * Datastar's form content type honours the renderer's `novalidate` and appends the submitter's
  * name/value to the posted fields, so the plain-POST protocol — named submit buttons, the
  * `__submit` discriminator — rides through unchanged.
  */
object DatastarFormTransport extends FormTransport:

    /** The header Datastar sends with every backend action request. */
    val requestHeader: (String, String) = "datastar-request" -> "true"

    def formAttributes(postAction: String): Seq[Modifier] =
        val post = s"@post('$postAction', {contentType: 'form'})"
        val committed = FormTransport.rerenderSelectors.mkString(", ")
        Seq(
            // Form-level submit handling: Datastar prevents the native submission itself,
            // so submit-type buttons keep their native semantics and no per-button wiring
            dataOn("submit") := post,
            dataOn("change") := s"""evt.target.matches("$committed") && $post"""
        )
    end formAttributes
end DatastarFormTransport
