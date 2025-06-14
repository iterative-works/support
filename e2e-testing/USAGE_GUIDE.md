# IW Support E2E Testing Framework Usage Guide

## Introduction

The `iw-support-e2e-testing` framework provides a streamlined way to write browser-based end-to-end tests using Playwright and Cucumber BDD in Scala projects. This guide will walk you through setting up and using the framework effectively.

## Installation and Setup

### Step 1: Add the Dependency

Add the following to your project's `build.sbt`:

```scala
libraryDependencies += "works.iterative" %% "iw-support-e2e-testing" % "0.1.0-medeca"
```

### Step 2: Configure Your Tests

Create `src/test/resources/application.conf` with your test configuration:

```hocon
# Required: Base URL of your application
baseUrl = "https://your-app.example.com/"

# Optional: Run tests in headless mode (default: true)
headless = false

# Optional: Slow down actions for debugging (milliseconds)
slowMo = 50.0

# Optional: Default timeout for actions (milliseconds)
timeout = 30000

# Optional: Browser viewport size
viewport {
  width = 1280
  height = 720
}

# Optional: Record videos of test runs
recordVideo = "target/videos"

# Optional: Screenshot configuration
screenshot {
  path = "target/screenshots"
  fullPage = true
}
```

### Step 3: Create Lifecycle Hooks

Create `src/test/scala/stepdefs/LifecycleHooks.scala` to manage test lifecycle:

```scala
package stepdefs

import io.cucumber.scala.*
import works.iterative.testing.e2e.*
import com.typesafe.config.ConfigFactory

// IMPORTANT: This must be an object, not a class!
object LifecycleHooks extends ScalaDsl with EN with CS:
    BeforeAll { () =>
        // Load configuration from application.conf
        val config = loadConfig()
        PlaywrightTestContext.initialize(config)
    }: Unit
    
    AfterAll { () =>
        PlaywrightTestContext.cleanup()
    }: Unit
    
    // Optional: Clear test data between scenarios
    After { (_: Scenario) =>
        PlaywrightTestContext.getTestData().clear()
    }
    
    private def loadConfig(): E2ETestConfig =
        val typesafeConfig = ConfigFactory.load()
        E2ETestConfig(
            baseUrl = typesafeConfig.getString("baseUrl"),
            headless = if typesafeConfig.hasPath("headless") then 
                typesafeConfig.getBoolean("headless") else true
        )
```

## Writing Tests

### Basic Step Definitions

Create step definition classes by extending `PlaywrightCucumberRunner`:

```scala
package stepdefs

import works.iterative.testing.e2e.*
import com.microsoft.playwright.*
import com.microsoft.playwright.options.*

class LoginStepDefinitions extends PlaywrightCucumberRunner:
    
    Given("I am on the login page") { () =>
        page.navigate(s"$baseUrl/login")
        page.waitForLoadState(LoadState.NETWORKIDLE)
    }
    
    When("I enter username {string} and password {string}") { 
        (username: String, password: String) =>
        page.fill("input[name='username']", username)
        page.fill("input[name='password']", password)
    }
    
    When("I click the login button") { () =>
        page.getByRole(AriaRole.BUTTON, 
            Page.GetByRoleOptions().setName("Login")).click()
    }
    
    Then("I should be logged in") { () =>
        page.waitForURL("**/dashboard")
        assert(page.locator(".user-menu").isVisible(), 
            "User menu should be visible after login")
    }
```

### Using Test Data Store

Share data between step definitions:

```scala
class ShoppingCartSteps extends PlaywrightCucumberRunner:
    
    When("I add {int} items to cart") { (count: Int) =>
        // Store data for later use
        testData.put("cartItemCount", count)
        
        // Add items to cart...
        for (i <- 1 to count) {
            page.click(s".product:nth-child($i) .add-to-cart")
        }
    }
    
    Then("the cart should show {int} items") { (expectedCount: Int) =>
        // Retrieve stored data
        val addedCount = testData.get[Int]("cartItemCount")
            .getOrElse(0)
        
        assert(addedCount == expectedCount, 
            s"Expected $expectedCount items, but added $addedCount")
        
        val cartCount = page.locator(".cart-count").textContent().toInt
        assert(cartCount == expectedCount)
    }
```

### Multi-Language Support

The framework supports both English and Czech Cucumber keywords:

```scala
class MultiLanguageSteps extends PlaywrightCucumberRunner:
    
    // English steps
    Given("I am logged in") { () =>
        // Implementation
    }
    
    // Czech steps
    Pokud("jsem přihlášen") { () =>
        // Implementation
    }
    
    Když("kliknu na tlačítko {string}") { (button: String) =>
        page.getByText(button).click()
    }
    
    Pak("vidím zprávu {string}") { (message: String) =>
        assert(page.getByText(message).isVisible())
    }
```

### Creating Reusable Base Classes

For common functionality across multiple step definitions:

```scala
package support

import works.iterative.testing.e2e.*
import com.microsoft.playwright.*

abstract class BaseStepDefinitions extends PlaywrightCucumberRunner:
    
    protected def login(username: String, password: String): Unit =
        page.navigate(s"$baseUrl/login")
        page.fill("input[name='username']", username)
        page.fill("input[name='password']", password)
        page.click("button[type='submit']")
        page.waitForURL("**/dashboard")
    
    protected def logout(): Unit =
        page.click(".user-menu")
        page.click("a[href='/logout']")
        page.waitForURL("**/login")
```

Use in your step definitions:

```scala
package stepdefs

import support.BaseStepDefinitions

class UserSteps extends BaseStepDefinitions:
    
    Given("I am logged in as {string}") { (username: String) =>
        login(username, "password123")
    }
```

## Writing Feature Files

Create feature files in `src/test/resources/features/`:

```gherkin
# language: en
Feature: User Authentication
  As a user
  I want to log in to the application
  So that I can access my account

  Background:
    Given I am on the login page

  @smoke @authentication
  Scenario: Successful login
    When I enter username "testuser" and password "password123"
    And I click the login button
    Then I should be logged in
    And I should see "Welcome, testuser"

  @negative @authentication
  Scenario: Login with invalid credentials
    When I enter username "invalid" and password "wrong"
    And I click the login button
    Then I should see error "Invalid credentials"
    And I should remain on the login page
```

For Czech language features:

```gherkin
# language: cs
Požadavek: Správa uživatelského účtu
  Jako přihlášený uživatel
  Chci spravovat svůj účet
  Abych mohl aktualizovat své údaje

  Pozadí:
    Pokud jsem přihlášen jako "testuser"
    A jsem na stránce mého profilu

  @profile
  Scénář: Změna hesla
    Když kliknu na "Změnit heslo"
    A zadám současné heslo "password123"
    A zadám nové heslo "newPassword456"
    A potvrdím nové heslo "newPassword456"
    A kliknu na tlačítko "Uložit"
    Pak vidím zprávu "Heslo bylo úspěšně změněno"
```

## Advanced Patterns

### Page Object Pattern

Create page objects for better organization:

```scala
package pages

import works.iterative.testing.e2e.*
import com.microsoft.playwright.*
import com.microsoft.playwright.options.*

class DashboardPage(page: Page, baseUrl: String):
    
    def navigate(): Unit =
        page.navigate(s"$baseUrl/dashboard")
        page.waitForLoadState(LoadState.NETWORKIDLE)
    
    def getProjectCount(): Int =
        page.locator(".project-card").count()
    
    def openProject(projectName: String): Unit =
        page.getByRole(AriaRole.LINK, 
            Page.GetByRoleOptions().setName(projectName)).click()
        page.waitForLoadState()
    
    def isProjectVisible(projectName: String): Boolean =
        page.getByText(projectName).isVisible()
    
    def getNotificationCount(): Int =
        val text = page.locator(".notification-badge").textContent()
        if (text.isEmpty) 0 else text.toInt
```

Use in step definitions:

```scala
class DashboardSteps extends PlaywrightCucumberRunner:
    lazy val dashboard = DashboardPage(page, baseUrl)
    
    When("I open project {string}") { (projectName: String) =>
        dashboard.openProject(projectName)
    }
    
    Then("I should see {int} projects") { (count: Int) =>
        assert(dashboard.getProjectCount() == count)
    }
```

### Handling Dynamic Content

For applications with dynamic content:

```scala
class DynamicContentSteps extends PlaywrightCucumberRunner:
    
    Then("I wait for the data to load") { () =>
        // Wait for loading spinner to disappear
        page.waitForSelector(".loading-spinner", 
            Page.WaitForSelectorOptions()
                .setState(WaitForSelectorState.HIDDEN)
                .setTimeout(10000))
        
        // Wait for content to appear
        page.waitForSelector(".data-table", 
            Page.WaitForSelectorOptions()
                .setState(WaitForSelectorState.VISIBLE))
    }
    
    When("I search for {string}") { (searchTerm: String) =>
        page.fill("input[type='search']", searchTerm)
        
        // Wait for debounce
        Thread.sleep(500)
        
        // Wait for results to update
        page.waitForResponse(
            response => response.url().contains("/api/search") && 
                       response.status() == 200,
            () => {
                // Trigger search if needed
                page.press("input[type='search']", "Enter")
            }
        )
    }
```

### Working with Tables

For testing data tables:

```scala
class TableSteps extends PlaywrightCucumberRunner:
    
    Then("the table should contain the following data:") { 
        (dataTable: io.cucumber.datatable.DataTable) =>
        
        val expectedRows = dataTable.asMaps().asScala
        val tableRows = page.locator("table tbody tr").all().asScala
        
        assert(tableRows.size == expectedRows.size, 
            s"Expected ${expectedRows.size} rows but found ${tableRows.size}")
        
        expectedRows.zip(tableRows).foreach { case (expected, row) =>
            expected.asScala.foreach { case (column, value) =>
                val cellIndex = getColumnIndex(column)
                val cellText = row.locator(s"td:nth-child($cellIndex)")
                    .textContent().trim
                assert(cellText == value, 
                    s"Expected '$value' in column '$column' but found '$cellText'")
            }
        }
    }
    
    private def getColumnIndex(columnName: String): Int =
        val headers = page.locator("table thead th").all().asScala
        headers.indexWhere(_.textContent().trim == columnName) + 1
```

## Debugging Tips

### 1. Disable Headless Mode

In `application.conf`:
```hocon
headless = false
slowMo = 500.0  # Half second between actions
```

### 2. Add Screenshots on Failure

```scala
After { (scenario: Scenario) =>
    if (scenario.isFailed) {
        val timestamp = System.currentTimeMillis()
        val screenshotPath = s"target/screenshots/failure-$timestamp.png"
        page.screenshot(Page.ScreenshotOptions()
            .setPath(java.nio.file.Paths.get(screenshotPath))
            .setFullPage(true))
        println(s"Screenshot saved to: $screenshotPath")
    }
}
```

### 3. Use Browser Developer Tools

```scala
When("I debug the page") { () =>
    page.pause()  // This will pause execution and open DevTools
}
```

### 4. Add Logging

```scala
class DebugSteps extends PlaywrightCucumberRunner:
    
    When("I perform action") { () =>
        println(s"Current URL: ${page.url()}")
        println(s"Page title: ${page.title()}")
        
        // Log all visible buttons
        val buttons = page.locator("button").all().asScala
        buttons.foreach { button =>
            println(s"Button: ${button.textContent()}")
        }
        
        // Perform action...
    }
```

## Common Issues and Solutions

### Issue: "Failed to instantiate class"

**Cause**: Cucumber trying to instantiate an abstract class or a class in the wrong package.

**Solution**: 
- Move base classes to a different package (e.g., `support`)
- Make base classes abstract
- Ensure concrete step definitions are in `stepdefs` package

### Issue: "Static hooks can only be defined in a static context"

**Cause**: Lifecycle hooks defined in a class instead of an object.

**Solution**: Define hooks in an `object`:
```scala
object LifecycleHooks extends ScalaDsl with EN with CS:
    BeforeAll { () => ... }: Unit
```

### Issue: NullPointerException when accessing page

**Cause**: Trying to access page before initialization.

**Solution**: The framework now includes lazy initialization, but ensure your lifecycle hooks are properly set up.

### Issue: Czech language steps not recognized

**Cause**: Missing CS trait.

**Solution**: Ensure your runner extends CS trait (included in PlaywrightCucumberRunner).

## Best Practices

1. **Use Explicit Waits**: Always wait for elements or page states rather than using `Thread.sleep()`
2. **Keep Steps Atomic**: Each step should perform one action or assertion
3. **Use Page Objects**: Encapsulate page-specific logic
4. **Clean Up Test Data**: Clear test data between scenarios
5. **Tag Your Scenarios**: Use tags for organizing and filtering tests
6. **Handle Failures Gracefully**: Add proper error messages to assertions

## Running Tests

```bash
# Run all tests
sbt test

# Run specific feature file
sbt "testOnly * -- --features src/test/resources/features/login.feature"

# Run scenarios with specific tags
sbt "testOnly * -- --tags @smoke"

# Run scenarios excluding certain tags
sbt "testOnly * -- --tags 'not @slow'"

# Generate HTML reports
sbt "testOnly * -- --plugin html:target/cucumber-reports"
```

## Integration with CI/CD

For CI/CD pipelines:

1. Install Playwright browsers:
   ```bash
   npx playwright install chromium
   npx playwright install-deps chromium
   ```

2. Configure for headless mode:
   ```hocon
   headless = true
   recordVideo = "target/videos"
   ```

3. Archive test artifacts:
   ```yaml
   # Example for GitHub Actions
   - uses: actions/upload-artifact@v3
     if: always()
     with:
       name: test-results
       path: |
         target/screenshots/
         target/videos/
         target/cucumber-reports/
   ```

This completes the usage guide for the iw-support-e2e-testing framework. The framework provides a solid foundation for browser-based testing with the flexibility to extend and customize as needed.