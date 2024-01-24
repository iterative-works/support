package works.iterative.app

import zio.*
import com.raquo.waypoint.*
import works.iterative.core.UserMessage
import com.raquo.laminar.api.L.*
import works.iterative.core.MessageCatalogue
import works.iterative.app.PageRender

trait Connector[Env, P]:
    def connect(
        render: PageRender[P]
    )(using Router[P], MessageCatalogue): URIO[Env, PageRender[P]]
    def routes(base: String): List[Route[P, ?]] = Nil
    def breadcrumbs: PartialFunction[P, List[(UserMessage, P)]] =
        PartialFunction.empty
    def appMods: HtmlMod = emptyMod
end Connector

object Connector:
    extension [Env, P](c: Connector[Env, P])
        def widen[Env2 <: Env]: Connector[Env2, P] =
            c.asInstanceOf[Connector[Env2, P]]
end Connector
