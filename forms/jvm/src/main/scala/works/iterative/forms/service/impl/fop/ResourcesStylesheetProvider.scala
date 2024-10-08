package portaly.forms.service.impl.fop

import javax.xml.transform.Source

import zio.*
import javax.xml.transform.stream.StreamSource
import works.iterative.core.Language
import java.io.InputStream

class ResourcesStylesheetProvider extends ApacheFopStylesheetProvider:
    override def stylesheetFor(formId: String, lang: Option[Language]): Task[Source] =
        val languageSpecificName = lang.map(l => s"${formId}_${l.value}")
        val formSpecificName = s"${formId}"

        def loadResource(name: String): Task[Option[java.io.InputStream]] =
            ZIO.attempt(Option(getClass.getResourceAsStream(s"/${name}.xsl")))

        val fallbackStylesheet: ZIO[Any, Throwable, InputStream] =
            loadResource(ResourcesStylesheetProvider.fallbackId).someOrFail(
                new IllegalStateException("No stylesheet found")
            )

        val specificStylesheet: ZIO[Any, Throwable, Option[InputStream]] =
            loadResource(formSpecificName)

        val languageSpecificStylesheet: ZIO[Any, Throwable, Option[InputStream]] =
            languageSpecificName.map(loadResource).getOrElse(ZIO.none)

        val finalStyleSheet =
            languageSpecificStylesheet.someOrElseZIO(
                specificStylesheet.someOrElseZIO(
                    fallbackStylesheet
                )
            )

        finalStyleSheet.map(StreamSource(_))
    end stylesheetFor
end ResourcesStylesheetProvider

object ResourcesStylesheetProvider:
    val fallbackId = "form"

    val layer: TaskLayer[ApacheFopStylesheetProvider] =
        ZLayer {
            ZIO.whenZIO(ZIO.attempt(getClass().getResourceAsStream("/form.xsl") != null))(
                ZIO.succeed(new ResourcesStylesheetProvider)
            ).someOrFail(new IllegalStateException("Fallback stylesheet not found"))
        }
end ResourcesStylesheetProvider
