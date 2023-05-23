package works.iterative.ui.components.laminar.modules.formpage

import zio.prelude.*
import com.raquo.laminar.api.L.{*, given}
import works.iterative.ui.components.laminar.forms.FormBuilderModule
import works.iterative.ui.components.laminar.forms.Form
import works.iterative.ui.components.ComponentContext
import works.iterative.ui.components.laminar.ComputableComponent
import works.iterative.ui.components.laminar.forms.FormBuilderContext

trait FormPageView[T: Form]:
  self: FormPageModel[T] with FormBuilderModule =>

  class View(model: Signal[Model], actions: Observer[Action])(using
      ctx: ComponentContext[_],
      fctx: FormBuilderContext
  ):

    val element: HtmlElement =
      ComputableComponent(div)(
        model.map(
          _.initialValue.map(renderForm).map(div(_))
        )
      ).element

    def renderForm(
        initialValue: Option[T]
    ): Seq[HtmlElement] =
      buildForm[T](summon[Form[T]], actions.contramap(Action.Submit(_)))
        .build(initialValue)
        .elements
