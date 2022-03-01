package cz.e_bs.cmi.mdr.pdb.app.components.form

import com.raquo.laminar.api.L.{*, given}

object FormBody:
  def apply(sections: HtmlElement*): HtmlElement =
    div(
      cls := "space-y-8 divide-y divide-gray-200 sm:space-y-5",
      sections
    )