package portaly.forms.service
package impl.fop

import zio.*
import javax.xml.transform.Source
import works.iterative.core.Language
import javax.xml.transform.TransformerFactory

trait ApacheFopStylesheetProvider:
    def stylesheetFor(formId: String, lang: Option[Language]): Task[Source]
    def newTransformerFactory: Task[TransformerFactory]

object ApacheFopStylesheetProvider:
    def stylesheetFor(
        formId: String,
        lang: Option[Language]
    ): RIO[ApacheFopStylesheetProvider, Source] =
        ZIO.serviceWithZIO(_.stylesheetFor(formId, lang))
end ApacheFopStylesheetProvider
