package works.iterative.testing.e2e

import com.microsoft.playwright.*
import com.microsoft.playwright.options.ViewportSize
import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters.*

case class E2ETestConfig(
    baseUrl: String,
    headless: Boolean = true,
    slowMo: Option[Double] = None,
    timeout: Option[Int] = Some(30000),
    viewport: Option[ViewportConfig] = None,
    recordVideo: Option[String] = None,
    screenshot: Option[ScreenshotConfig] = None,
    locale: Option[String] = None
)

case class ViewportConfig(width: Int, height: Int)
case class ScreenshotConfig(path: String, fullPage: Boolean = false)

// scalafix:off DisableSyntax.var DisableSyntax.null
// Playwright Java interop: mutable state required for lifecycle management of browser resources
object PlaywrightTestContext:
    import scala.compiletime.uninitialized
    private var playwright: Playwright = uninitialized
    private var browser: Browser = uninitialized
    private var context: BrowserContext = uninitialized
    private var page: Page = uninitialized
    private val testData = new TestDataStore()
    private var configInstance: E2ETestConfig = uninitialized
    
    def initialize(config: E2ETestConfig): Unit =
        configInstance = config
        playwright = Playwright.create()
        
        val launchOptions = new BrowserType.LaunchOptions()
            .setHeadless(config.headless)
        config.slowMo.foreach(launchOptions.setSlowMo(_))
        browser = playwright.chromium().launch(launchOptions)
        
        val contextOptions = new Browser.NewContextOptions()
        config.viewport.foreach { vp =>
            contextOptions.setViewportSize(new ViewportSize(vp.width, vp.height))
        }
        config.recordVideo.foreach { path =>
            contextOptions.setRecordVideoDir(java.nio.file.Paths.get(path))
        }
        config.locale.foreach { locale =>
            contextOptions.setLocale(locale)
        }
        context = browser.newContext(contextOptions)
        page = context.newPage()
    
    def getPage(): Page = 
        if (configInstance == null) {
            // Lazy initialization if not initialized yet
            val config = loadDefaultConfig()
            initialize(config)
        }
        page
    
    def getContext(): BrowserContext = context
    
    def getBrowser(): Browser = browser
    
    def getTestData(): TestDataStore = testData
    
    def getConfig(): E2ETestConfig = 
        if (configInstance == null) {
            // Lazy initialization if not initialized yet
            val config = loadDefaultConfig()
            initialize(config)
        }
        configInstance
    
    private def loadDefaultConfig(): E2ETestConfig =
        import com.typesafe.config.ConfigFactory
        val typesafeConfig = ConfigFactory.load()
        E2ETestConfig(
            baseUrl = typesafeConfig.getString("baseUrl"),
            headless = if typesafeConfig.hasPath("headless") then typesafeConfig.getBoolean("headless") else true,
            slowMo = if typesafeConfig.hasPath("slowMo") then Some(typesafeConfig.getDouble("slowMo")) else None,
            timeout = if typesafeConfig.hasPath("timeout") then Some(typesafeConfig.getInt("timeout")) else Some(30000),
            viewport = if typesafeConfig.hasPath("viewport.width") && typesafeConfig.hasPath("viewport.height") then
                Some(ViewportConfig(
                    typesafeConfig.getInt("viewport.width"),
                    typesafeConfig.getInt("viewport.height")
                ))
            else None,
            recordVideo = if typesafeConfig.hasPath("recordVideo") then Some(typesafeConfig.getString("recordVideo")) else None,
            screenshot = if typesafeConfig.hasPath("screenshot.path") then
                Some(ScreenshotConfig(
                    typesafeConfig.getString("screenshot.path"),
                    if typesafeConfig.hasPath("screenshot.fullPage") then typesafeConfig.getBoolean("screenshot.fullPage") else false
                ))
            else None,
            locale = if typesafeConfig.hasPath("locale") then Some(typesafeConfig.getString("locale")) else None
        )
    
    def newPage(): Page = context.newPage()
    
    def cleanup(): Unit =
        if (page != null) page.close()
        if (context != null) context.close()
        if (browser != null) browser.close()
        if (playwright != null) playwright.close()
        testData.clear()
// scalafix:on DisableSyntax.var DisableSyntax.null

class TestDataStore:
    private val data = new ConcurrentHashMap[String, Any]()
    
    def put[A](key: String, value: A): Unit = 
        data.put(key, value): Unit
    
    def get[A](key: String): Option[A] = 
        Option(data.get(key)).map(_.asInstanceOf[A])
    
    def remove(key: String): Unit = 
        data.remove(key): Unit
    
    def clear(): Unit = 
        data.clear()
    
    def getAll: Map[String, Any] = 
        data.asScala.toMap