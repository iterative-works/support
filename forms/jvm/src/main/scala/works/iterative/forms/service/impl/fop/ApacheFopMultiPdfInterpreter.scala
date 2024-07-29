package portaly.forms
package service
package impl.fop

import zio.*
import works.iterative.core.MessageCatalogue
import portaly.forms.impl.FormR
import org.apache.fop.apps.FopFactory
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import org.apache.xmlgraphics.util.MimeConstants
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamSource
import scala.xml.*
import works.iterative.autocomplete.service.AutocompleteService
import works.iterative.core.Language
import scala.annotation.nowarn

final case class ApacheFopConfig(
    baseDir: Option[java.nio.file.Path],
    fopConfig: Option[java.nio.file.Path]
)

object ApacheFopConfig:
    val config: Config[ApacheFopConfig] =
        import Config.*
        (
            string("base").map(java.nio.file.Path.of(_)).optional ++ string("config").map(
                java.nio.file.Path.of(_)
            ).optional
        ).nested("fop").map(ApacheFopConfig.apply)
    end config
end ApacheFopConfig

// Scala XML compiler bug: https://github.com/scala/bug/issues/12658
@nowarn("msg=unused value of type scala.xml.NodeBuffer")
class ApacheFopMultiPdfInterpreter(
    fopFactory: FopFactory,
    builder: UIFormBuilder,
    displayResolver: UIXMLDisplayResolver,
    autocompleteResolver: AutocompleteResolver,
    autocompleteService: AutocompleteService,
    stylesheetProvider: ApacheFopStylesheetProvider
) extends MultiPdfInterpreter:
    import MultiPdfInterpreter.*
    def interpret(
        forms: List[FormRender],
        messages: MessageCatalogue,
        lang: Language
    ): UIO[Chunk[Byte]] = {
        given MessageCatalogue = messages
        given Language = lang

        def renderOne(form: FormRender): Task[NodeSeq] =
            val data = FormR.parse(form.data.getOrElse(Map.empty))

            val uiForm = builder.buildForm(
                form.form,
                data,
                FormValidationState.valid
            )

            for
                xml <- UIFormXMLRenderer(
                    autocompleteService,
                    autocompleteResolver,
                    displayResolver,
                    data
                ).render(uiForm)
            yield xml
            end for
        end renderOne

        def renderForms: Task[NodeSeq] =
            for
                xmls <- ZIO.foreach(forms)(renderOne)
            yield <ui:forms xmlns:ui="https://ui.iterative.works/form">{xmls}</ui:forms>

        def renderPdf(xml: NodeSeq): Task[Array[Byte]] = ZIO.scoped {
            for
                bout <- ZIO.fromAutoCloseable(ZIO.attempt(new ByteArrayOutputStream()))
                out <- ZIO.fromAutoCloseable(ZIO.attempt(new BufferedOutputStream(bout)))
                xslt <- stylesheetProvider.stylesheetFor(
                    forms.head.form.id.serialize,
                    if lang == Language.CS then None else Some(lang)
                )
                transformerFactory <- stylesheetProvider.newTransformerFactory
                // Render PDF using Apache FOP
                _ <- ZIO.attempt {
                    val fop = fopFactory.newFop(MimeConstants.MIME_PDF, out)
                    val transformer = transformerFactory.newTransformer(xslt)
                    val src = new javax.xml.transform.stream.StreamSource(
                        new java.io.StringReader(xml.toString)
                    )
                    val res = new javax.xml.transform.sax.SAXResult(fop.getDefaultHandler)
                    transformer.transform(src, res)
                }
            yield bout
        }.map(_.toByteArray())

        for
            xml <- renderForms
            pdf <- renderPdf(xml)
        yield Chunk.fromArray(pdf)
        end for
    }.orDie
end ApacheFopMultiPdfInterpreter

object ApacheFopMultiPdfInterpreter:
    val layer: RLayer[
        MessageCatalogue &
            UIXMLDisplayResolver & AutocompleteResolver & ApacheFopStylesheetProvider &
            AutocompleteService & UIFormBuilder,
        MultiPdfInterpreter
    ] = ZLayer(ZIO.config(ApacheFopConfig.config)).flatMap(configEnv => layer(configEnv.get))

    def layer(config: ApacheFopConfig): RLayer[
        MessageCatalogue &
            UIXMLDisplayResolver & AutocompleteResolver & ApacheFopStylesheetProvider &
            AutocompleteService & UIFormBuilder,
        MultiPdfInterpreter
    ] =
        ZLayer {
            for
                given MessageCatalogue <- ZIO.service[MessageCatalogue]
                builder <- ZIO.service[UIFormBuilder]
                displayResolver <- ZIO.service[UIXMLDisplayResolver]
                autocompleteResolver <- ZIO.service[AutocompleteResolver]
                autocompleteService <- ZIO.service[AutocompleteService]
                stylessheetProvider <- ZIO.service[ApacheFopStylesheetProvider]
                fopFactory <- (config match
                    case ApacheFopConfig(_, Some(cfg)) =>
                        ZIO.log(s"Using custom FOP configuration from ${cfg}") *>
                            ZIO.attempt(FopFactory.newInstance(cfg.toFile))
                    case ApacheFopConfig(Some(bd), _) =>
                        ZIO.log(
                            "Using default FOP configuration with base in ${bd}"
                        ) *> ZIO.attempt:
                            FopFactory.newInstance(bd.toUri)
                    case _ =>
                        val baseDir = new java.io.File(".").toURI()
                        for
                            _ <- ZIO.log(
                                s"Using FOP configuration with base in ${baseDir} and fop.xconf in classpath"
                            )
                            fopFactory <- ZIO.attempt(FopFactory.newInstance(
                                baseDir,
                                config.getClass.getResourceAsStream("/fop.xconf")
                            ))
                        yield fopFactory
                        end for
                ).orDie
            yield ApacheFopMultiPdfInterpreter(
                fopFactory,
                builder,
                displayResolver,
                autocompleteResolver,
                autocompleteService,
                stylessheetProvider
            )
        }
end ApacheFopMultiPdfInterpreter
