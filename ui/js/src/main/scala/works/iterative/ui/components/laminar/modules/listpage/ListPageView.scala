package works.iterative.ui.components.laminar.modules.listpage

import zio.prelude.*
import com.raquo.laminar.api.L.{*, given}
import works.iterative.ui.components.ComponentContext
import works.iterative.ui.components.laminar.ComputableComponent
import works.iterative.ui.components.laminar.HtmlTabular
import io.laminext.syntax.core.*
import works.iterative.ui.components.laminar.tables.HtmlTableBuilderModule

trait ListPageView[T: HtmlTabular]:
  self: ListPageModel[T] with HtmlTableBuilderModule =>

  class View(model: Signal[Model], actions: Observer[Action])(using
      ctx: ComponentContext[_]
  ):

    val element: HtmlElement =
      ComputableComponent(div)(
        model.map(_.items.map(renderItem))
      ).element

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
