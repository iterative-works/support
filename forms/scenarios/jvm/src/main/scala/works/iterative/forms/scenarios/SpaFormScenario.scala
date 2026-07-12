// PURPOSE: Scenario proving the client-side form loop: the custom element renders the proof form
// PURPOSE: through LiveHtmlInterpreter and submits validated data back to this server

package works.iterative.forms.scenarios

object SpaFormScenario extends SpaScenario(
        "spaForm",
        "SPA Form",
        "iw-form",
        "inquiry",
        "proof",
        InquiryProofForm.declaration
    )
