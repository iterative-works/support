// PURPOSE: SPA page for the conformance vocabulary form — the client-side half of the browser pair
// PURPOSE: The custom element must render the same controls and validation the SSR page does

package works.iterative.forms.scenarios

object SpaVocabularyScenario extends SpaScenario(
        "spaVocab",
        "SPA Vocabulary",
        "iw-vocab-form",
        "vocab",
        "conformance",
        ConformanceCorpus.vocabularyForm
    )
