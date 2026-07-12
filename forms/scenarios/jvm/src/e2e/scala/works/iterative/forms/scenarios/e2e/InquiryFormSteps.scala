// PURPOSE: Step definitions driving the proof form in both variants — SSR page and SPA custom element
// PURPOSE: One wording, two mechanics: steps branch on the open page so the features read identically

package works.iterative.forms.scenarios.e2e

import com.microsoft.playwright.*
import com.microsoft.playwright.options.*
import scala.jdk.CollectionConverters.*
import works.iterative.testing.e2e.*

class InquiryFormSteps extends PlaywrightCucumberRunner:

    private def spa: Boolean = page.url().contains("/spaForm/")

    // Hidden __items inputs carry repeated-row state as "key:type"; row N maps to the Nth key
    private def itemKeys: List[String] =
        page.locator("input[name='inquiry.items.__items']").all().asScala
            .flatMap(_.inputValue().split(",").filter(_.nonEmpty))
            .map(_.takeWhile(_ != ':')).toList

    private def rowField(row: Int, field: String): Locator =
        page.locator(s"input[name='inquiry.items.${itemKeys(row - 1)}.row.$field']")

    Given("the inquiry form is open") { () =>
        page.navigate(s"$baseUrl/ssrForm/page")
        page.waitForLoadState(LoadState.NETWORKIDLE)
    }

    Given("the SPA inquiry form is open") { () =>
        page.navigate(s"$baseUrl/spaForm/page")
        // The custom element fetches the declaration and renders client-side
        page.waitForSelector("input[name='inquiry.customer.name']"): Unit
    }

    When("I choose {string} as the request kind") { (kind: String) =>
        if spa then page.click(s"#inquiry-request-kind-$kind")
        else page.selectOption("select[name='inquiry.request.kind']", kind): Unit
        // The re-rendered summary proves the change landed (htmx swap or reactive update)
        page.waitForSelector(
            s"#inquiry-request-summary:has-text('You are requesting a $kind')"
        ): Unit
    }

    When("I fill in {string} with {string}") { (label: String, value: String) =>
        page.getByLabel(label).fill(value)
    }

    When("I mark the inquiry as urgent") { () =>
        if spa then page.click("input[name='inquiry.request.urgent']")
        else
            page.selectOption("select[name='inquiry.request.urgent']", "true"): Unit
            // The change-triggered swap re-renders the form; the selected option proves it
            // landed. Options never count as visible, so wait for attachment only.
            page.waitForSelector(
                "select[name='inquiry.request.urgent'] option[value='true'][selected]",
                Page.WaitForSelectorOptions().setState(WaitForSelectorState.ATTACHED)
            ): Unit
    }

    When("I set the deadline to {string}") { (date: String) =>
        page.getByLabel("Deadline").fill(date)
        // Date changes swap the SSR form; the re-rendered value attribute proves it landed
        if !spa then
            page.waitForSelector(s"input[name='inquiry.request.deadline'][value='$date']"): Unit
    }

    When("I submit the form") { () =>
        if spa then page.click("#inquiry-submit")
        else page.click("button[name='__submit']")
    }

    When("I add an item row") { () =>
        val before = itemKeys.size
        if spa then page.click("#inquiry-items-row-add")
        else page.click("button[name='inquiry.controls.addItem']")
        page.waitForCondition(() => itemKeys.size == before + 1)
    }

    When("I remove item row {int}") { (row: Int) =>
        val key = itemKeys(row - 1)
        if spa then page.click(s"#inquiry-items-$key-row span.bg-red-700")
        else page.click(s"button[name='inquiry.items.$key.row.remove']")
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
        val expected = if spa then "/spaForm/page" else "/ssrForm/page"
        assert(
            page.url().endsWith(expected),
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

    Then("the received {string} is {string}") { (key: String, value: String) =>
        val dump = page.locator("pre code").textContent()
        val entry = s""""$key":["$value"]"""
        assert(dump.contains(entry), s"Expected submitted data to contain $entry, but was: $dump")
    }
end InquiryFormSteps
