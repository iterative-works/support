// PURPOSE: Step definitions driving the vocabulary conformance form in both variants
// PURPOSE: One wording, two mechanics: steps branch on the open page so the pins read identically

package works.iterative.forms.scenarios.e2e

import com.microsoft.playwright.*
import com.microsoft.playwright.options.*
import io.cucumber.datatable.DataTable
import scala.jdk.CollectionConverters.*
import works.iterative.testing.e2e.*

class VocabularyFormSteps extends PlaywrightCucumberRunner:

    private def spa: Boolean = page.url().contains("/spaVocab/")

    Given("the SSR vocabulary form is open") { () =>
        page.navigate(s"$baseUrl/ssrVocab/page")
        page.waitForLoadState(LoadState.NETWORKIDLE)
    }

    Given("the SPA vocabulary form is open") { () =>
        page.navigate(s"$baseUrl/spaVocab/page")
        // The custom element fetches the declaration and renders client-side
        page.waitForSelector("input[name='vocab.string']"): Unit
    }

    Then("the vocabulary renders these controls:") { (table: DataTable) =>
        table.asLists().asScala.foreach { row =>
            val (fieldId, control) = (row.get(0), row.get(1))
            val selector = control match
                case "textarea"         => s"textarea[name='vocab.$fieldId']"
                case s"input:$inputType" =>
                    s"input[name='vocab.$fieldId'][type='$inputType']"
                case other => throw new IllegalArgumentException(s"Unknown control: $other")
            // Hidden inputs never count as visible; attachment is the rendering proof
            page.waitForSelector(
                selector,
                Page.WaitForSelectorOptions().setState(WaitForSelectorState.ATTACHED)
            ): Unit
        }
    }

    When("I submit the vocabulary form") { () =>
        // SSR: the declared Submit button is the submit control (chrome is suppressed);
        // SPA: declared buttons stay with the client's handler, the element chrome submits
        if spa then page.click("#vocab-submit")
        else page.click("#vocab-send")
    }

    When("I click the vocabulary button {string}") { (label: String) =>
        page.getByRole(
            AriaRole.BUTTON,
            Page.GetByRoleOptions().setName(label)
        ).click()
    }

    Then("the vocabulary is received") { () =>
        page.locator("h1", Page.LocatorOptions().setHasText("Vocabulary received"))
            .waitFor()
    }

    Then("the declared Submit button is the only submit control") { () =>
        val declared = page.locator("button#vocab-send[name='__submit']")
        declared.waitFor()
        val submits = page.locator("button[name='__submit']").count()
        assert(submits == 1, s"Expected exactly one submit control, found $submits")
    }

    Then("the vocabulary form is still shown") { () =>
        val field = page.locator("input[name='vocab.string']")
        field.waitFor()
        assert(field.isVisible(), "Vocabulary form should still be shown")
    }
end VocabularyFormSteps
