package cz.e_bs.cmi.mdr.pdb.app.components

import com.raquo.laminar.api.L.{*, given}

trait PageLayout {
  def navigation: HtmlElement
  def pageHeader: HtmlElement

  def render(
      $m: Signal[Option[HtmlElement]],
      mods: Modifier[HtmlElement]*
  ): HtmlElement =
    val $maybeContent = $m.split(_ => ())((_, c, _) => c)
    div(
      cls := "min-h-full",
      navigation,
      pageHeader,
      main(
        mods,
        child <-- $maybeContent.map(_.getOrElse(Loading))
      )
    )
}
