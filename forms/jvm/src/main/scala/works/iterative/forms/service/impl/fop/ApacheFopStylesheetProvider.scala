package portaly.forms.service
package impl.fop

import zio.*
import javax.xml.transform.Source
import works.iterative.core.Language

trait ApacheFopStylesheetProvider:
    def stylesheetFor(formId: String, lang: Option[Language]): Task[Source]

object ApacheFopStylesheetProvider:
    def stylesheetFor(
        formId: String,
        lang: Option[Language]
    ): RIO[ApacheFopStylesheetProvider, Source] =
        ZIO.serviceWithZIO(_.stylesheetFor(formId, lang))
end ApacheFopStylesheetProvider
