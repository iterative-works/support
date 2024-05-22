package works.iterative.ui.components
package laminar
package modules
package formpage

import zio.prelude.*
import com.raquo.laminar.api.L.*
import works.iterative.ui.components.laminar.forms.*

trait FormPageView[T: Form]:
  self: FormPageModel[T] with FormBuilderModule with ComputableComponents =>

  class View(model: Signal[Model], actions: Observer[Action])(using
      fctx: FormBuilderContext
  ):

    val element: HtmlElement =
      renderComputable(
        model.map(
          _.initialValue.map(renderForm).map(div(_))
        )
      )

    def renderForm(
        initialValue: Option[T]
    ): Seq[HtmlElement] =
      buildForm[T](
        summon[Form[T]],
        actions.contracollect[Form.Event[T]] { case Form.Event.Submitted(a) =>
          Action.Submit(a)
        }
      )
        .build(initialValue)
        .elements
