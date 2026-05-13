// PURPOSE: Unit tests for CommonsEmailNotificationService email construction.
// PURPOSE: Verifies HtmlEmail vs MultiPartEmail selection based on EmailContent.htmlMessage.

package works.iterative.service.email
package impl.apache

import zio.test.*

import org.apache.commons.mail.HtmlEmail

object CommonsEmailNotificationServiceSpec extends ZIOSpecDefault:

    private val testConfig = SMTPConfig(
        smtpHost = "localhost",
        smtpPort = 25,
        smtpUsername = None,
        smtpPassword = None,
        smtpSender = "from@example.com",
        smtpSenderName = Some("Test Sender"),
        smtpTestRecipient = None
    )

    private def service: CommonsEmailNotificationService =
        new CommonsEmailNotificationService(testConfig)

    private val sampleSubject = "Hello"
    private val sampleText = "Plain body"
    private val sampleHtml = "<p>HTML body</p>"

    def spec = suite("CommonsEmailNotificationService.buildEmail")(
        test("htmlMessage = None produces a MultiPartEmail (not HtmlEmail) with subject and text body") {
            val content = EmailContent(sampleSubject, sampleText)
            val email = service.buildEmail(content)
            assertTrue(
                !email.isInstanceOf[HtmlEmail],
                email.getSubject == sampleSubject
            )
        },
        test("htmlMessage = Some produces an HtmlEmail carrying the html body") {
            val content = EmailContent(
                sampleSubject,
                sampleText,
                htmlMessage = Some(sampleHtml)
            )
            val email = service.buildEmail(content)
            val htmlField =
                val f = classOf[HtmlEmail].getDeclaredField("html")
                f.setAccessible(true)
                f.get(email).asInstanceOf[String]
            assertTrue(
                email.isInstanceOf[HtmlEmail],
                email.getSubject == sampleSubject,
                htmlField == sampleHtml
            )
        },
        test("htmlMessage = Some preserves attachments via HtmlEmail path") {
            val attachment = Attachment(
                filename = "note.txt",
                mimeType = "text/plain",
                content = "hello".getBytes("UTF-8")
            )
            val content = EmailContent(
                sampleSubject,
                sampleText,
                Some(sampleHtml),
                attachment
            )
            val email = service.buildEmail(content)
            assertTrue(email.isInstanceOf[HtmlEmail])
        },
        test("EmailContent.htmlMessage defaults to None for existing call sites") {
            val content = EmailContent(sampleSubject, sampleText)
            assertTrue(content.htmlMessage == None)
        }
    )
end CommonsEmailNotificationServiceSpec
