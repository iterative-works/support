package cz.e_bs.cmi.mdr.pdb.waypoint.components

import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.Router
import org.scalajs.dom

trait Navigator[P](using router: Router[P]):
  def navigateTo(page: P): Binder[HtmlElement] = Navigator.navigateTo[P](page)

// TODO: replace router NavigateTo action
object Navigator {
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
