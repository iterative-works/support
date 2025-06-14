# iw-support-e2e-testing

A simple E2E testing framework that integrates Playwright browser automation with Cucumber BDD testing.

## Features

- **Playwright Browser Automation**: Modern browser automation with auto-wait strategies
- **Cucumber BDD Support**: Write tests in Gherkin syntax with Scala step definitions
- **Configuration-Driven**: Externalize test configuration using Typesafe Config
- **Test Data Management**: Share data between test steps within scenarios
- **Common Step Definitions**: Pre-built reusable step definitions for common operations
- **Page Object Pattern Support**: Optional page object pattern for better test organization

## Setup

Add the dependency to your `build.sbt`:

```scala
libraryDependencies += "works.iterative.support" %% "iw-support-e2e-testing" % "0.1.10-medeca-SNAPSHOT" % Test
```

## Configuration

Create an `application.conf` file in your test resources:

```hocon
{
  baseUrl = "https://example.com"
  headless = true
  slowMo = 100  # Optional: slow down operations by 100ms
  timeout = 30000  # Default timeout in milliseconds
  
  # Optional viewport configuration
  viewport {
    width = 1280
    height = 720
  }
  
  # Optional video recording
  recordVideo = "test-videos"
  
  # Optional screenshot configuration
  screenshot {
    path = "test-screenshots"
    fullPage = false
  }
}
```

## Usage

### Basic Setup

Create a test runner that extends `PlaywrightCucumberRunner`:

```scala
import works.iterative.testing.e2e.*
import io.cucumber.junit.{Cucumber, CucumberOptions}
import org.junit.runner.RunWith

@RunWith(classOf[Cucumber])
@CucumberOptions(
    features = Array("src/test/resources/features"),
    glue = Array("com.example.steps"),
    plugin = Array("pretty", "html:target/cucumber-reports")
)
class E2ETestRunner

// Step definitions
package com.example.steps

import works.iterative.testing.e2e.*

class MyStepDefinitions extends PlaywrightCucumberRunner with CommonStepDefinitions:
    
    Given("I am logged in as {string}") { (username: String) =>
        page.navigate("/login")
        page.fill("#username", username)
        page.fill("#password", "test-password")
        page.click("button[type='submit']")
        page.waitForSelector(".dashboard")
    }
```

### Using Common Step Definitions

The framework provides many pre-built step definitions:

```gherkin
Feature: User Login
  
  Scenario: Successful login
    Given I navigate to "/login"
    When I fill in "username" with "testuser"
    And I fill in "password" with "password123"
    And I click the "Login" button
    Then I should see "Welcome, testuser"
    And I should be on the "dashboard" page
```

### Using Page Objects

For better test organization, you can use the page object pattern:

```scala
import works.iterative.testing.e2e.*
import com.microsoft.playwright.*

class LoginPage(val page: Page, baseUrl: String) extends BasePageObject(baseUrl, "/login"):
    private val usernameField = "#username"
    private val passwordField = "#password"
    private val loginButton = "button[type='submit']"
    private val errorMessage = ".error-message"
    
    def login(username: String, password: String): Unit =
        fillField(usernameField, username)
        fillField(passwordField, password)
        click(loginButton)
    
    def getErrorMessage: String =
        getText(errorMessage)

// Using in step definitions
Given("I log in with valid credentials") { () =>
    val loginPage = new LoginPage(page, baseUrl)
    loginPage.navigate()
    loginPage.login("testuser", "password123")
}
```

### Test Data Sharing

Share data between steps using the test data store:

```scala
When("I create a new item") { () =>
    // Your item creation code
    val itemId = page.locator(".item-id").textContent()
    testData.put("createdItemId", itemId)
}

Then("I can view the created item") { () =>
    val itemId = testData.get[String]("createdItemId")
        .getOrElse(throw new Exception("No item ID found"))
    page.navigate(s"/items/$itemId")
    page.waitForSelector(".item-details")
}
```

### Custom Configuration

You can override the configuration loading:

```scala
class MyStepDefinitions extends PlaywrightCucumberRunner:
    override protected def loadConfig(): E2ETestConfig = 
        E2ETestConfig(
            baseUrl = sys.env.getOrElse("TEST_BASE_URL", "http://localhost:3000"),
            headless = sys.env.get("SHOW_BROWSER").isEmpty,
            slowMo = if (sys.env.contains("DEBUG")) Some(500.0) else None
        )
```

## Best Practices

1. **Use Configuration Files**: Externalize URLs, credentials, and other environment-specific settings
2. **Leverage Common Steps**: Use the provided common step definitions before writing custom ones
3. **Page Objects for Complex Pages**: Use page objects when pages have complex interactions
4. **Test Data Cleanup**: The framework automatically clears test data between scenarios
5. **Meaningful Step Names**: Write steps that describe business behavior, not technical details

## Migration from Existing Tests

To migrate existing Playwright/Cucumber tests:

1. Replace test context/browser management with `PlaywrightTestContext`
2. Extend `PlaywrightCucumberRunner` instead of managing browser lifecycle manually
3. Use configuration files instead of hardcoded values
4. Leverage common step definitions where applicable

Example migration:

```scala
// Before
object TestContext {
    private var browser: Browser = _
    def getPage(): Page = ...
}

// After
class MySteps extends PlaywrightCucumberRunner:
    // Use page directly instead of TestContext.getPage()
```