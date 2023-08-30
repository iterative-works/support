package works.iterative.ui.components.laminar

import com.raquo.laminar.api.L.*

/** A way to add effect processor to component.
  *
  * To ensure that the lifecycle of the effect processor is bound to the
  * lifecycle of the component, the bind is done in the component itself.
  *
  * There is a default implementation for components that return HtmlElement. If
  * the component uses different type, it needs to provide its own
  * implementation.
  */
trait HasEffectHook[O]:
  extension (o: O) def amend(mod: HtmlMod): O

object HasEffectHook:
  given HasEffectHook[HtmlElement] with
    extension (e: HtmlElement)
      def amend(mod: HtmlMod): HtmlElement =
        e.amend(mod)
