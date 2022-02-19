package cz.e_bs.cmi.mdr.pdb.app.components
package list

import com.raquo.laminar.api.L.{*, given}

object RowNext:
  def render: HtmlElement =
    div(
      cls := "flex-shrink-0",
      Icons.solid.`chevron-right`
    )
