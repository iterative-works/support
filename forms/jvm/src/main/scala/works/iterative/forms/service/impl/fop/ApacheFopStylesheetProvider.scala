package portaly.forms.service
package impl.fop

import zio.*
import javax.xml.transform.Source
import works.iterative.core.Language
import javax.xml.transform.TransformerFactory

/*
  Provides XSLT stylesheets for Apache FOP.

  And also provides a new TransformerFactory, as it needs configuration for the imports, and the stylesheet
  provider is the one that should provide it.
 */
trait ApacheFopStylesheetProvider:
    def stylesheetFor(formId: String, lang: Option[Language]): Task[Source]
    def newTransformerFactory: Task[TransformerFactory]
end ApacheFopStylesheetProvider

object ApacheFopStylesheetProvider:
    def stylesheetFor(
        formId: String,
        lang: Option[Language]
    ): RIO[ApacheFopStylesheetProvider, Source] =
        ZIO.serviceWithZIO(_.stylesheetFor(formId, lang))

    def newTransformerFactory: RIO[ApacheFopStylesheetProvider, TransformerFactory] =
        ZIO.serviceWithZIO(_.newTransformerFactory)
end ApacheFopStylesheetProvider
