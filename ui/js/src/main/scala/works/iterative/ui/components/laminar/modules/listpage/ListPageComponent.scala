package works.iterative.ui.components.laminar.modules.listpage

import com.raquo.laminar.api.L.*
import works.iterative.ui.components.laminar.{EffectHandler, LaminarComponent}
import works.iterative.ui.components.laminar.tables.HtmlTableBuilderModule
import works.iterative.ui.components.ComponentContext

trait ListPageComponent[T]:
  self: ListPageView[T] with ListPageModel[T, ?] with HtmlTableBuilderModule =>

  class Component(effectHandler: EffectHandler[Effect, Action])(using
      ComponentContext[?]
  ) extends LaminarComponent[Model, Action, Effect, HtmlElement](
        effectHandler
      )
      with Module:
    override def render(
        m: Signal[Model],
        actions: Observer[Action]
    ): HtmlElement =
      View(m, actions).element
