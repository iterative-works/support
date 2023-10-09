package works.iterative.ui.components
package laminar
package modules
package listpage

import zio.prelude.*
import com.raquo.laminar.api.L.*
import io.laminext.syntax.core.*
import works.iterative.ui.components.laminar.tables.HtmlTableBuilderModule

trait ListPageView[T: HtmlTabular]:
  self: ListPageModel[T, ?] with HtmlTableBuilderModule with ComputableComponents =>

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
          nodeSeq(
            // TODO: no styling info in the generic module
            cls("cursor-pointer hover:bg-gray-100"),
            onClick.mapTo(Action.VisitDetail(item)) --> actions
          )
        )
        .build
