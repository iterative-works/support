# E2E Testing Framework Example

This example shows how to set up e2e tests in a new project using the iw-support-e2e-testing framework.

## Project Setup

### 1. Add Dependency

In your `build.sbt`:

```scala
lazy val `my-e2e-tests` = (project in file("e2e-tests"))
    .settings(
        libraryDependencies ++= Seq(
            supportLib("e2e-testing", supportVersion),
            "com.github.sbt" % "junit-interface" % "0.13.3" % Test
        )
    )
```

### 2. Create Test Configuration

Create `e2e-tests/src/test/resources/application.conf`:

```hocon
{
  baseUrl = "http://localhost:3000/"
  baseUrl = ${?E2E_BASE_URL}
  
  headless = true
  headless = ${?E2E_HEADLESS}
  
  viewport {
    width = 1280
    height = 720
  }
}
```

### 3. Create Test Runner

Create `e2e-tests/src/test/scala/MyE2ERunner.scala`:

```scala
import io.cucumber.junit.{Cucumber, CucumberOptions}
import org.junit.runner.RunWith

@RunWith(classOf[Cucumber])
@CucumberOptions(
    features = Array("e2e-tests/src/test/resources/features"),
    glue = Array("steps"),
    plugin = Array("pretty", "html:target/cucumber-reports")
)
class MyE2ERunner
```

### 4. Create Base Step Definitions

Create `e2e-tests/src/test/scala/steps/BaseSteps.scala`:

```scala
package steps

import works.iterative.testing.e2e.*
import zio.*

abstract class BaseSteps extends PlaywrightCucumberRunner with CommonStepDefinitions:
    override protected def testLayer = E2ETestLayers.testEnvironment
```

### 5. Create Custom Step Definitions

Create `e2e-tests/src/test/scala/steps/LoginSteps.scala`:

```scala
package steps

import works.iterative.testing.e2e.*
import com.microsoft.playwright.*
import zio.*

class LoginSteps extends BaseSteps:
    
    Given("I am logged in as {string}") { (username: String) =>
        withPage { page =>
            for
                _ <- ZIO.attempt(page.navigate("/login"))
                _ <- ZIO.attempt(page.fill("#username", username))
                _ <- ZIO.attempt(page.fill("#password", "test-password"))
                _ <- ZIO.attempt(page.click("button[type='submit']"))
                _ <- ZIO.attempt(page.waitForSelector(".dashboard"))
            yield ()
        }
    }
```

### 6. Write Feature Files

Create `e2e-tests/src/test/resources/features/login.feature`:

```gherkin
Feature: User Login

  Scenario: Successful login
    Given I navigate to "/login"
    When I fill in "username" with "testuser"
    And I fill in "password" with "correct-password"
    And I click the "Login" button
    Then I should see "Welcome"
    And I should not see "Invalid credentials"
```

### 7. Run Tests

```bash
# Run all e2e tests
sbt e2e-tests/test

# Run with visible browser
E2E_HEADLESS=false sbt e2e-tests/test

# Run specific scenario
sbt -Dcucumber.filter.name="Successful login" e2e-tests/test

# Run specific feature file
sbt -Dcucumber.features="e2e-tests/src/test/resources/features/login.feature" e2e-tests/test
```

## Advanced Usage

### Using Page Objects

```scala
import works.iterative.testing.e2e.*
import com.microsoft.playwright.*

class DashboardPage(val page: Page, baseUrl: String) 
    extends ZIOPageObject(baseUrl, "/dashboard"):
    
    def getProjectCount: Task[Int] =
        ZIO.attempt(page.locator(".project-card").count())
    
    def selectProject(name: String): Task[Unit] =
        click(s"text=$name")

object DashboardPage extends PageObjectFactory[DashboardPage]:
    def create(page: Page): DashboardPage = 
        new DashboardPage(page, "http://localhost:3000")

// Use in steps
Given("I select project {string}") { (projectName: String) =>
    PageObjectSupport.withPageObject(DashboardPage) { dashboard =>
        for
            _ <- dashboard.navigate()
            _ <- dashboard.selectProject(projectName)
        yield ()
    }
}
```

### Custom Configuration

```scala
class MySteps extends PlaywrightCucumberRunner:
    override protected def testLayer = 
        E2ETestLayers.withConfig(
            baseUrl = sys.env.getOrElse("BASE_URL", "http://localhost:3000"),
            headless = !sys.env.contains("SHOW_BROWSER"),
            slowMo = if sys.env.contains("DEBUG") Some(1000) else None,
            timeout = Some(60000)
        )
```

### Sharing Data Between Steps

```scala
When("I create a new item named {string}") { (name: String) =>
    withPage { page =>
        for
            ctx <- ZIO.service[PlaywrightTestContext]
            _ <- ZIO.attempt(page.fill("#item-name", name))
            _ <- ZIO.attempt(page.click("button.create"))
            itemId <- ZIO.attempt(page.locator(".item-id").textContent())
            _ <- ZIO.succeed(ctx.testData.put("createdItemId", itemId))
            _ <- ZIO.succeed(ctx.testData.put("createdItemName", name))
        yield ()
    }
}

Then("I can view the created item") { () =>
    withPage { page =>
        for
            ctx <- ZIO.service[PlaywrightTestContext]
            itemId <- ZIO.fromOption(ctx.testData.get[String]("createdItemId"))
                .orElseFail(new Exception("No item created"))
            itemName <- ZIO.fromOption(ctx.testData.get[String]("createdItemName"))
                .orElseFail(new Exception("No item name"))
            _ <- ZIO.attempt(page.navigate(s"/items/$itemId"))
            _ <- ZIO.attempt(page.getByText(itemName).waitFor())
        yield ()
    }
}
```