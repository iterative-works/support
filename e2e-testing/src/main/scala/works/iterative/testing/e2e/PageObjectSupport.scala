package works.iterative.testing.e2e

import com.microsoft.playwright.*
import com.microsoft.playwright.options.*

trait PageObject:
    def page: Page
    def url: String
    
    def navigate(): Unit = 
        page.navigate(url)
        page.waitForLoadState(LoadState.NETWORKIDLE)
    
    def isCurrentPage: Boolean =
        page.url().contains(url)
    
    def waitForElement(selector: String): Locator =
        val locator = page.locator(selector)
        locator.waitFor()
        locator
    
    def fillField(selector: String, value: String): Unit =
        page.locator(selector).fill(value)
    
    def click(selector: String): Unit =
        page.locator(selector).click()
    
    def getText(selector: String): String =
        page.locator(selector).textContent()
    
    def isVisible(selector: String): Boolean =
        page.locator(selector).isVisible()
    
    def selectOption(selector: String, value: String): Unit =
        page.locator(selector).selectOption(value): Unit
    
    def check(selector: String): Unit =
        page.locator(selector).check()
    
    def uncheck(selector: String): Unit =
        page.locator(selector).uncheck()

abstract class BasePageObject(protected val baseUrl: String, path: String) extends PageObject:
    def url: String = s"$baseUrl$path"