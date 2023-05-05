package works.iterative.ui.components.laminar

import com.raquo.laminar.api.L.HtmlMod
import works.iterative.ui.model.tables.Tabular

/** A tabular typclass that can be rendered into HTML */
trait HtmlTabular[A] extends Tabular[A, HtmlMod]
