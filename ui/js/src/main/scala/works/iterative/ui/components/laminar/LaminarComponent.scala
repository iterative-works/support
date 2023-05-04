package works.iterative.ui
package components.laminar

import com.raquo.laminar.api.L.{*, given}

abstract class LaminarComponent[M, A, E](
    effectHandler: EffectHandler[E, A]
) extends Module[M, A, E]:
  def render(m: Signal[M], actions: Observer[A]): HtmlElement

  val element: HtmlElement =
    val actions = new EventBus[A]

    val zero @ (_, effect) = init

    val initialEffect$ = (effect match
      case Some(e) => EventStream.fromValue(e)
      case _       => EventStream.empty
    )

    val actions$ = actions.events.recover(handleFailure)

    val processor$ = actions$.scanLeft(zero) { case ((m, _), a) =>
      handle(a, m)
    }

    val nextEffects$ = processor$.changes.collect { case (_, Some(e)) => e }

    val model$ = processor$.map(_._1)

    val effect$ = EventStream.merge(initialEffect$, nextEffects$)

    render(model$, actions.writer).amend(
      effectHandler(effect$, actions.writer)
    )
