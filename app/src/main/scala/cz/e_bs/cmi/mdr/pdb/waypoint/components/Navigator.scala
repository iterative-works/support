package cz.e_bs.cmi.mdr.pdb.waypoint.components

import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.Router
import org.scalajs.dom
import com.raquo.domtypes.jsdom.defs.events.TypedTargetMouseEvent
import com.raquo.laminar.nodes.ReactiveElement

trait Navigator[P](using router: Router[P]):
  def navigateTo(page: P): Binder[HtmlElement] = Navigator.navigateTo[P](page)

// TODO: replace router NavigateTo action
object Navigator {
  def navigateTo[P]($page: Signal[P])(using
      router: Router[P]
  ): Binder[HtmlElement] =
    Binder { el =>

      val isLinkElement = el.ref.isInstanceOf[dom.html.Anchor]

      if (isLinkElement) {
        el.amend(href <-- $page.map(page => router.absoluteUrlForPage(page)))
      }

      // If element is a link and user is holding a modifier while clicking:
      //  - Do nothing, browser will open the URL in new tab / window / etc. depending on the modifier key
      // Otherwise:
      //  - Perform regular pushState transition
      (
        composeEvents(
          onClick
            .filter(ev =>
              !(isLinkElement && (ev.ctrlKey || ev.metaKey || ev.shiftKey || ev.altKey))
            )
            .preventDefault
        )(_.sample($page)) --> (p => router.pushState(p))
      ).bind(el)
    }

  def navigateTo[P](page: P)(using router: Router[P]): Binder[HtmlElement] =
    Binder { el =>

      val isLinkElement = el.ref.isInstanceOf[dom.html.Anchor]

      if (isLinkElement) {
        el.amend(href(router.absoluteUrlForPage(page)))
      }

      // If element is a link and user is holding a modifier while clicking:
      //  - Do nothing, browser will open the URL in new tab / window / etc. depending on the modifier key
      // Otherwise:
      //  - Perform regular pushState transition
      (onClick
        .filter(ev =>
          !(isLinkElement && (ev.ctrlKey || ev.metaKey || ev.shiftKey || ev.altKey))
        )
        .preventDefault
        --> (_ => router.pushState(page))).bind(el)
    }
}
