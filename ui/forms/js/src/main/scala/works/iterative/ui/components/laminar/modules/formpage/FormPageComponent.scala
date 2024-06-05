package works.iterative.ui.components.laminar.modules.formpage

import com.raquo.laminar.api.L.*
import works.iterative.ui.components.laminar.EffectHandler
import works.iterative.ui.components.laminar.LaminarComponent
import works.iterative.ui.components.laminar.forms.FormBuilderModule
import works.iterative.ui.components.laminar.forms.FormBuilderContext

trait FormPageComponent[T]:
    self: FormPageModel[T] & FormPageView[T] & FormBuilderModule =>

    class Component(effectHandler: EffectHandler[Effect, Action])(using
        fctx: FormBuilderContext
    ) extends LaminarComponent[Model, Action, Effect, HtmlElement](effectHandler)
        with Module:
        override def render(
            m: Signal[Model],
            actions: Observer[Action]
        ): HtmlElement =
            View(m, actions).element
    end Component
end FormPageComponent
