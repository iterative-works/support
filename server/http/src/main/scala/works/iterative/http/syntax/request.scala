package works.iterative.http.syntax

import org.http4s.Request
import works.iterative.core.Language
import works.iterative.core.MessageCatalogue
import works.iterative.core.service.MessageCatalogueService
import org.http4s.headers.`Accept-Language`
import zio.*

object request:
    extension [F[_]](req: Request[F])
        def language(defaultLanguage: Language = Language.CS): Language =
            req.cookies
                .find(_.name == "lang")
                .flatMap(c => Language(c.content).toOption)
                .orElse(
                    req.headers
                        .get[`Accept-Language`]
                        .flatMap(l => Language(l.values.head.primaryTag).toOption)
                )
                .getOrElse(defaultLanguage)

        def messageCatalogueFromService(
            service: MessageCatalogueService,
            defaultLanguage: Language = Language.CS
        ): UIO[MessageCatalogue] =
            service.forLanguage(req.language(defaultLanguage))

        def messageCatalogue(defaultLanguage: Language = Language.CS)
            : ZIO[MessageCatalogueService, Nothing, MessageCatalogue] =
            ZIO.serviceWithZIO[MessageCatalogueService](
                _.forLanguage(req.language(defaultLanguage))
            )
    end extension
end request
