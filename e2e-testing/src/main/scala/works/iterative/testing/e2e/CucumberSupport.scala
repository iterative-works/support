package works.iterative.testing.e2e

import io.cucumber.scala.*
import com.microsoft.playwright.*

/**
 * Base trait for Cucumber test runners using Playwright.
 * 
 * This trait provides access to Playwright page objects and test data.
 * The lifecycle hooks (BeforeAll, AfterAll, After) are defined in the
 * CucumberLifecycleHooks object to satisfy Cucumber's requirement for
 * static hooks to be in a static context.
 */
trait PlaywrightCucumberRunner extends ScalaDsl with EN with CS:
    protected def page: Page = PlaywrightTestContext.getPage()
    protected def freshPage(): Page = PlaywrightTestContext.newPage()
    protected def testData: TestDataStore = PlaywrightTestContext.getTestData()
    protected def baseUrl: String = PlaywrightTestContext.getConfig().baseUrl
    protected def config: E2ETestConfig = PlaywrightTestContext.getConfig()
