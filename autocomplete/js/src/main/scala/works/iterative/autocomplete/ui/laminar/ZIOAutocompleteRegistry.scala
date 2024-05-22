package works.iterative.autocomplete.ui.laminar

import zio.*
import com.raquo.laminar.api.L.*
import works.iterative.autocomplete.AutocompleteEntry
import works.iterative.autocomplete.service.AutocompleteService
import works.iterative.ui.components.laminar.forms.FieldId
import works.iterative.ui.laminar.*
import works.iterative.app.LanguageService

class ZIOAutocompleteRegistry(
    service: AutocompleteService,
    // TODO: replace with iw-core LanguageService
    languageService: LanguageService,
    mapping: PartialFunction[FieldId, ZIOAutocompleteRegistry.Config],
    context: Option[Map[String, String]] = None
)(using Runtime[Any]) extends AutocompleteRegistry:
    import ZIOAutocompleteRegistry.AutocompleteQueryImpl

    override def getQueryFor(id: FieldId): Option[AutocompleteQuery] = mapping.lift(id).map: c =>
        new AutocompleteQueryImpl(
            service,
            languageService,
            c,
            context,
            Val(None)
        )

    override def withContext(ctx: Option[Map[String, String]]): AutocompleteRegistry =
        new ZIOAutocompleteRegistry(service, languageService, mapping, ctx)

    override def addContext(ctx: Map[String, String]): AutocompleteRegistry =
        new ZIOAutocompleteRegistry(
            service,
            languageService,
            mapping,
            context match
                case Some(c) => Some(c ++ ctx)
                case _       => Some(ctx)
        )
end ZIOAutocompleteRegistry

object ZIOAutocompleteRegistry:
    final case class Config(
        collection: String,
        limit: Int = 20,
        strict: Boolean = false,
        context: Option[Map[String, String]] = None
    )

    def layer(mapping: PartialFunction[FieldId, Config])
        : URLayer[AutocompleteService & LanguageService, AutocompleteRegistry] =
        ZLayer {
            for
                service <- ZIO.service[AutocompleteService]
                languageService <- ZIO.service[LanguageService]
                given Runtime[Any] <- ZIO.runtime[Any]
            yield ZIOAutocompleteRegistry(service, languageService, mapping)
        }

    def composeContexts(
        c1: Option[Map[String, String]],
        c2: Option[Map[String, String]]
    ): Option[Map[String, String]] = (c1, c2) match
        case (Some(a), Some(b)) => Some(a ++ b)
        case (Some(a), _)       => Some(a)
        case (_, b)             => b

    class AutocompleteQueryImpl(
        service: AutocompleteService,
        languageService: LanguageService,
        config: Config,
        context: Option[Map[String, String]],
        additionalContextSignal: Signal[Option[Map[String, String]]]
    )(using Runtime[Any]) extends AutocompleteQuery:
        val finalContext = composeContexts(context, config.context)

        override def find(q: String): EventStream[List[AutocompleteEntry]] =
            additionalContextSignal.flatMapSwitch(add =>
                service.find(
                    config.collection,
                    q,
                    config.limit,
                    languageService.currentLanguage,
                    composeContexts(finalContext, add)
                ).map(_.toList).toEventStream
            )

        override def load(id: String): EventStream[Option[AutocompleteEntry]] =
            service.load(config.collection, id, languageService.currentLanguage, context).map {
                case None if !config.strict => Some(AutocompleteEntry(id, id, None, Map.empty))
                case c                      => c
            }.toEventStream

        override def withContextSignal(ctx: Signal[Option[Map[String, String]]])
            : AutocompleteQuery =
            new AutocompleteQueryImpl(service, languageService, config, context, ctx)
    end AutocompleteQueryImpl
end ZIOAutocompleteRegistry
