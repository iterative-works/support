package works.iterative.ui.laminar

import com.raquo.laminar.api.L.*

package object headless:
    val dataState = dataAttr("iw-ui-state")

    def closeOnClickOutside(open: Var[Boolean]): Binder[HtmlElement] = Binder {
        n =>
            (windowEvents(_.onClick)
                .filterWith(open.signal)
                .map(_.target)
                .collect { case el: org.scalajs.dom.HTMLElement =>
                    el
                } --> open.writer
                .contramap[org.scalajs.dom.HTMLElement](el =>
                    n.ref.contains(el)
                ))
                .bind(n)
    }
end headless
