// PURPOSE: Seam for declared form buttons — an interpreter registers each button through
// PURPOSE: a handler that either passes the click on or answers it with value mutations
package portaly.forms
package impl

import com.raquo.laminar.api.L.*
import works.iterative.ui.laminar.*
import works.iterative.ui.model.forms.AbsolutePath

trait ButtonHandler:
    def register(btn: AbsolutePath)(using FormCtx): HtmlMod

object ButtonHandler:
    val empty: ButtonHandler = new ButtonHandler:
        def register(btn: AbsolutePath)(using FormCtx): HtmlMod = emptyMod
    enum Result:
        case Passed(btn: AbsolutePath)
        case Handled(btn: AbsolutePath, mutations: EventStream[FormR])
end ButtonHandler
