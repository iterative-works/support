package cz.e_bs.cmi.mdr.pdb.app.components

import com.raquo.laminar.api.L.{*, given}

trait PageLayout {
  def navigation: HtmlElement
  def pageHeader: HtmlElement
  def pageContent: HtmlElement

  def render: HtmlElement =
    div(
      cls := "min-h-full",
      navigation,
      pageHeader,
      main(pageContent)
    )
}
