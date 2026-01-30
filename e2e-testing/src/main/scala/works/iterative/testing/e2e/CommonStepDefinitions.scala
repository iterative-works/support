package works.iterative.testing.e2e

import com.microsoft.playwright.*
import com.microsoft.playwright.options.*
import io.cucumber.scala.*

/**
 * Common step definitions for Playwright E2E tests.
 * 
 * Mix this trait into your step definition classes to get pre-built steps.
 * 
 * Example usage:
 * ```scala
 * class MyStepDefinitions extends PlaywrightCucumberRunner with CommonStepDefinitions:
 *     // Your custom step definitions...
 * ```
 */
// scalafix:off DisableSyntax.throw
// Cucumber step definitions: exceptions are the idiomatic error reporting mechanism
trait CommonStepDefinitions:
    self: PlaywrightCucumberRunner =>

    // Navigation steps
    Given("^I navigate to \"([^\"]*)\"$") { (path: String) =>
        val url = s"$baseUrl$path"
        page.navigate(url)
        page.waitForLoadState(LoadState.NETWORKIDLE)
    }

    Given("^I am on the \"([^\"]*)\" page$") { (pageName: String) =>
        val path = testData.get[String](s"${pageName}Path")
            .getOrElse(throw new Exception(s"Unknown page: $pageName"))
        val url = s"$baseUrl$path"
        page.navigate(url)
        page.waitForLoadState(LoadState.NETWORKIDLE)
    }
    
    // Form interaction steps
    When("^I fill in \"([^\"]*)\" with \"([^\"]*)\"$") { (field: String, value: String) =>
        val input = page.locator(s"input[name='$field'], input[id='$field'], input[placeholder='$field']").first()
        input.fill(value)
    }
    
    When("^I select \"([^\"]*)\" from \"([^\"]*)\"$") { (value: String, field: String) =>
        val select = page.locator(s"select[name='$field'], select[id='$field']").first()
        select.selectOption(value)
    }
    
    When("^I check \"([^\"]*)\"$") { (field: String) =>
        val checkbox = page.locator(s"input[type='checkbox'][name='$field'], input[type='checkbox'][id='$field']").first()
        checkbox.check()
    }
    
    When("^I uncheck \"([^\"]*)\"$") { (field: String) =>
        val checkbox = page.locator(s"input[type='checkbox'][name='$field'], input[type='checkbox'][id='$field']").first()
        checkbox.uncheck()
    }
    
    // Click actions
    When("^I click on \"([^\"]*)\"$") { (text: String) =>
        page.getByText(text, new Page.GetByTextOptions().setExact(true)).click()
    }
    
    When("^I click the \"([^\"]*)\" button$") { (buttonText: String) =>
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(buttonText)).click()
    }
    
    When("^I click the link \"([^\"]*)\"$") { (linkText: String) =>
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(linkText)).click()
    }
    
    // Waiting steps
    When("^I wait for (\\d+) seconds?$") { (seconds: Int) =>
        Thread.sleep(seconds * 1000)
    }
    
    When("^I wait for \"([^\"]*)\" to be visible$") { (selector: String) =>
        page.locator(selector).waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE))
    }
    
    When("^I wait for \"([^\"]*)\" to disappear$") { (selector: String) =>
        page.locator(selector).waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN))
    }
    
    // Assertions
    Then("^I should see \"([^\"]*)\"$") { (text: String) =>
        page.getByText(text).waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE))
    }
    
    Then("^I should not see \"([^\"]*)\"$") { (text: String) =>
        val locator = page.getByText(text)
        if (locator.count() > 0) {
            locator.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN))
        }
    }
    
    Then("^I should be on the \"([^\"]*)\" page$") { (pageName: String) =>
        val expectedPath = testData.get[String](s"${pageName}Path")
            .getOrElse(throw new Exception(s"Unknown page: $pageName"))
        val currentUrl = page.url()
        assert(currentUrl.contains(expectedPath), s"Expected to be on $expectedPath but was on $currentUrl")
    }
    
    Then("^the \"([^\"]*)\" field should contain \"([^\"]*)\"$") { (field: String, expectedValue: String) =>
        val input = page.locator(s"input[name='$field'], input[id='$field']").first()
        val actualValue = input.inputValue()
        assert(actualValue == expectedValue, s"Expected '$expectedValue' but got '$actualValue'")
    }
    
    // Store data between steps
    When("^I store the value of \"([^\"]*)\" as \"([^\"]*)\"$") { (selector: String, key: String) =>
        val value = page.locator(selector).textContent()
        testData.put(key, value)
    }
    
    When("^I use the stored value \"([^\"]*)\" in \"([^\"]*)\"$") { (key: String, field: String) =>
        val value = testData.get[String](key)
            .getOrElse(throw new Exception(s"No stored value for key: $key"))
        val input = page.locator(s"input[name='$field'], input[id='$field']").first()
        input.fill(value)
    }
    
    // Screenshot support
    When("^I take a screenshot named \"([^\"]*)\"$") { (filename: String) =>
        val screenshotPath = config.screenshot.map(_.path).getOrElse("screenshots")
        val path = java.nio.file.Paths.get(screenshotPath, s"$filename.png")
        page.screenshot(new Page.ScreenshotOptions().setPath(path))
    }
// scalafix:on DisableSyntax.throw
