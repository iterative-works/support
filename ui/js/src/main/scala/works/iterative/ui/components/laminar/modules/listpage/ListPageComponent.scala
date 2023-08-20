package works.iterative.ui.components.laminar.modules.listpage

import com.raquo.laminar.api.L.{*, given}
import works.iterative.ui.components.laminar.EffectHandler
import works.iterative.ui.components.laminar.LaminarComponent
import works.iterative.ui.components.ComponentContext
import works.iterative.ui.components.laminar.tables.HtmlTableBuilderModule

trait ListPageComponent[T]:
  self: ListPageView[T] with ListPageModel[T] with HtmlTableBuilderModule =>

  class Component(effectHandler: EffectHandler[Effect, Action])(using
      ctx: ComponentContext[_]
  ) extends LaminarComponent[Model, Action, Effect, HtmlElement](effectHandler)
      with Module:
    override def render(
        m: Signal[Model],
        actions: Observer[Action]
    ): HtmlElement =
      View(m, actions).element
