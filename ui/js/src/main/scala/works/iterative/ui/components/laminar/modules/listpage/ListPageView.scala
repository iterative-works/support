package works.iterative.ui.components
package laminar
package modules
package listpage

import zio.prelude.*
import com.raquo.laminar.api.L.*
import works.iterative.ui.components.laminar.tables.HtmlTableBuilderModule

trait ListPageView[T: HtmlTabular]:
    self: ListPageModel[T, ?] & HtmlTableBuilderModule & ComputableComponents =>

    @scala.annotation.nowarn("msg=unused implicit parameter")
    class View(model: Signal[Model], actions: Observer[Action])(using
        ComponentContext[?]
    ):

        val element: HtmlElement =
            renderComputable(
                model.map(_.items.map(renderItem))
            )

        private def renderItem(items: List[T]): HtmlElement =
            buildTable(items)
                .dataRowMod(item =>
                    modSeq(
                        // TODO: no styling info in the generic module
                        cls("cursor-pointer hover:bg-gray-100"),
                        onClick.mapTo(Action.VisitDetail(item)) --> actions
                    )
                )
                .build
    end View
end ListPageView
