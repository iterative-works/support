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
        unique: Boolean = false,
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
        override val strict = config.strict

        val finalContext = composeContexts(
            composeContexts(context, config.context),
            if config.unique then Some(Map("unique" -> "true")) else None
        )

        private val findCache = Unsafe.unsafe:
            Ref.Synchronized.unsafe.make(Map.empty[Find, Promise[Nothing, List[AutocompleteEntry]]])

        private val loadCache = Unsafe.unsafe:
            Ref.Synchronized.unsafe.make(Map.empty[Load, Promise[
                Nothing,
                Option[AutocompleteEntry]
            ]])

        sealed trait Operation[M[_]]

        final case class Find(
            collection: String,
            q: String,
            limit: Int,
            language: String,
            contexts: Option[Map[String, String]]
        ) extends Operation[List]

        final case class Load(
            collection: String,
            id: String,
            language: String,
            context: Option[Map[String, String]]
        ) extends Operation[Option]

        override def find(q: String): EventStream[List[AutocompleteEntry]] =
            EventStream.unit().withCurrentValueOf(additionalContextSignal.signal).flatMapSwitch(
                add =>
                    cached(Find(
                        config.collection,
                        q,
                        config.limit,
                        languageService.currentLanguage,
                        composeContexts(finalContext, add)
                    )).toEventStream
            )
        end find

        override def load(id: String): EventStream[Option[AutocompleteEntry]] =
            cached(Load(config.collection, id, languageService.currentLanguage, context)).map {
                case None if !config.strict => Some(AutocompleteEntry(id, id, None, Map.empty))
                case c                      => c
            }.toEventStream

        override def withContextSignal(ctx: Signal[Option[Map[String, String]]])
            : AutocompleteQuery =
            new AutocompleteQueryImpl(service, languageService, config, context, ctx)

        private def cached[M[_]](op: Operation[M]): UIO[M[AutocompleteEntry]] =
            // Naive cache implementation, could be improved, but it's good enough for now
            op match
                case key @ Find(collection, q, limit, language, contexts) =>
                    for
                        promise <- findCache.modifyZIO: entries =>
                            // We lookup the key, if found, we return the map as is
                            // If not found, we create the effect that will add the promise to the map
                            entries.get(key) match
                                case Some(p) => ZIO.succeed((p, entries))
                                case None =>
                                    for
                                        p <- Promise.make[Nothing, List[AutocompleteEntry]]
                                        _ <- p.complete(service.find(
                                            collection,
                                            q,
                                            limit,
                                            language,
                                            contexts
                                        )).fork
                                    yield (p, entries + (key -> p))
                        result <- promise.await
                    yield result
                case key @ Load(collection, id, language, context) =>
                    for
                        promise <- loadCache.modifyZIO: entries =>
                            entries.get(key) match
                                case Some(p) => ZIO.succeed((p, entries))
                                case None =>
                                    for
                                        p <- Promise.make[Nothing, Option[AutocompleteEntry]]
                                        _ <- p.complete(service.load(
                                            collection,
                                            id,
                                            language,
                                            context
                                        )).fork
                                    yield (p, entries + (key -> p))
                        result <- promise.await
                    yield result
    end AutocompleteQueryImpl
end ZIOAutocompleteRegistry
