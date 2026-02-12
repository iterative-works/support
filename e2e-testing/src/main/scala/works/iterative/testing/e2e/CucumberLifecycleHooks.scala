package works.iterative.testing.e2e

import io.cucumber.scala.*
import com.typesafe.config.ConfigFactory

/** Singleton object that contains the lifecycle hooks for Cucumber tests. This must be an object
  * (not a class) because Cucumber requires static hooks to be defined in a static context.
  */
object CucumberLifecycleHooks extends ScalaDsl with EN with CS:
    BeforeAll {
        val config = loadConfig()
        PlaywrightTestContext.initialize(config)
    }

    AfterAll {
        PlaywrightTestContext.cleanup()
    }

    After { (_: Scenario) =>
        PlaywrightTestContext.getTestData().clear()
    }

    private def loadConfig(): E2ETestConfig =
        val typesafeConfig = ConfigFactory.load()
        E2ETestConfig(
            baseUrl = typesafeConfig.getString("baseUrl"),
            headless = if typesafeConfig.hasPath("headless") then
                typesafeConfig.getBoolean("headless")
            else true,
            slowMo = if typesafeConfig.hasPath("slowMo") then
                Some(typesafeConfig.getDouble("slowMo"))
            else None,
            timeout = if typesafeConfig.hasPath("timeout") then
                Some(typesafeConfig.getInt("timeout"))
            else Some(30000),
            viewport =
                if typesafeConfig.hasPath("viewport.width") && typesafeConfig.hasPath(
                        "viewport.height"
                    )
                then
                    Some(ViewportConfig(
                        typesafeConfig.getInt("viewport.width"),
                        typesafeConfig.getInt("viewport.height")
                    ))
                else None,
            recordVideo = if typesafeConfig.hasPath("recordVideo") then
                Some(typesafeConfig.getString("recordVideo"))
            else None,
            screenshot = if typesafeConfig.hasPath("screenshot.path") then
                Some(ScreenshotConfig(
                    typesafeConfig.getString("screenshot.path"),
                    if typesafeConfig.hasPath("screenshot.fullPage") then
                        typesafeConfig.getBoolean("screenshot.fullPage")
                    else false
                ))
            else None,
            locale = if typesafeConfig.hasPath("locale") then
                Some(typesafeConfig.getString("locale"))
            else None
        )
    end loadConfig
end CucumberLifecycleHooks
