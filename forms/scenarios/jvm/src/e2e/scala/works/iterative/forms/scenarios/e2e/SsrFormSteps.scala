// PURPOSE: Step definitions for the SSR inquiry form features
// PURPOSE: Selectors derive from IdPath names; waits target server-rendered outcomes, never timing

package works.iterative.forms.scenarios.e2e

import com.microsoft.playwright.*
import com.microsoft.playwright.options.*
import scala.jdk.CollectionConverters.*
import works.iterative.testing.e2e.*

class SsrFormSteps extends PlaywrightCucumberRunner:

    // Hidden __items inputs carry repeated-row state as "key:type"; row N maps to the Nth key
    private def itemKeys: List[String] =
        page.locator("input[name='inquiry.items.__items']").all().asScala
            .map(_.inputValue().takeWhile(_ != ':')).toList

    private def rowField(row: Int, field: String): Locator =
        page.locator(s"input[name='inquiry.items.${itemKeys(row - 1)}.row.$field']")

    Given("the inquiry form is open") { () =>
        page.navigate(s"$baseUrl/ssrForm/page")
        page.waitForLoadState(LoadState.NETWORKIDLE)
    }

    When("I choose {string} as the request kind") { (kind: String) =>
        page.selectOption("select[name='inquiry.request.kind']", kind): Unit
        // htmx swaps the whole form; the re-rendered summary proves the swap landed
        page.waitForSelector(
            s"#inquiry-request-summary:has-text('You are requesting a $kind')"
        ): Unit
    }

    When("I fill in {string} with {string}") { (label: String, value: String) =>
        page.getByLabel(label).fill(value)
    }

    When("I submit the form") { () =>
        page.click("button[name='__submit']")
    }

    When("I add an item row") { () =>
        val before = itemKeys.size
        page.click("button[name='inquiry.controls.addItem']")
        page.waitForCondition(() => itemKeys.size == before + 1)
    }

    When("I remove item row {int}") { (row: Int) =>
        val key = itemKeys(row - 1)
        page.click(s"button[name='inquiry.items.$key.row.remove']")
        page.waitForCondition(() => !itemKeys.contains(key))
    }

    When("I fill item row {int} with quantity {string} and description {string}") {
        (row: Int, qty: String, desc: String) =>
            rowField(row, "qty").fill(qty)
            rowField(row, "desc").fill(desc)
    }

    Then("the delivery address field is visible") { () =>
        val address = page.locator("input[name='inquiry.request.delivery.address']")
        address.waitFor()
        assert(address.isVisible(), "Delivery address field should be visible")
    }

    Then("I am still on the form page") { () =>
        assert(
            page.url().endsWith("/ssrForm/page"),
            s"Expected to stay on the form page, but URL is ${page.url()}"
        )
    }

    Then("the summary reads {string}") { (text: String) =>
        val summary = page.locator("#inquiry-request-summary")
        summary.waitFor()
        val actual = summary.textContent()
        assert(actual.contains(text), s"Expected summary to read '$text', but was '$actual'")
    }

    Then("I see a validation error {string}") { (message: String) =>
        page.locator(".field-error", Page.LocatorOptions().setHasText(message))
            .first().waitFor()
    }

    Then("the field {string} contains {string}") { (label: String, value: String) =>
        val actual = page.getByLabel(label).inputValue()
        assert(actual == value, s"Expected field '$label' to contain '$value', but was '$actual'")
    }

    Then("the form has {int} item row(s)") { (count: Int) =>
        page.waitForCondition(() => itemKeys.size == count)
    }

    Then("item row {int} has quantity {string} and description {string}") {
        (row: Int, qty: String, desc: String) =>
            val actualQty = rowField(row, "qty").inputValue()
            val actualDesc = rowField(row, "desc").inputValue()
            assert(actualQty == qty, s"Expected quantity '$qty', but was '$actualQty'")
            assert(actualDesc == desc, s"Expected description '$desc', but was '$actualDesc'")
    }

    Then("the inquiry is received") { () =>
        page.locator("h1", Page.LocatorOptions().setHasText("Inquiry received"))
            .waitFor()
    }

    Then("the received data includes {string}") { (value: String) =>
        val dump = page.locator("pre code").textContent()
        assert(dump.contains(value), s"Expected submitted data to include '$value', but was: $dump")
    }
end SsrFormSteps
