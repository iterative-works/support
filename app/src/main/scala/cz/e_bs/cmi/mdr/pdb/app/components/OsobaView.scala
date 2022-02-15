package cz.e_bs.cmi.mdr.pdb.app.components

import com.raquo.laminar.api.L.{*, given}
import cz.e_bs.cmi.mdr.pdb.app.Osoba
import cz.e_bs.cmi.mdr.pdb.app.Funkce
import cz.e_bs.cmi.mdr.pdb.app.PracovniPomer
import CustomAttrs.datetime

// TODO: refactor to view model
def OsobaView($osoba: Signal[Osoba]): HtmlElement =
  def funkce($fce: Signal[Funkce]) =
    p(
      cls := "text-sm font-medium text-gray-500",
      child.text <-- $fce.map(_.nazev),
      span(
        cls := "hidden md:inline",
        " @ ",
        child.text <-- $fce.map(_.stredisko),
        ", ",
        child.text <-- $fce.map(_.voj)
      )
    )

  def pp($pp: Signal[PracovniPomer]) =
    p(
      cls := "text-sm font-medium text-gray-500",
      child.text <-- $pp.map(_.druh),
      " od ",
      time(
        datetime <-- $pp.map(_.pocatek.toString),
        child.text <-- $pp.map(_.pocatek.toString)
      )
    )

  div(
    cls := "md:flex md:items-center md:justify-between md:space-x-5",
    div(
      cls := "flex items-start space-x-5",
      div(
        cls := "flex-shrink-0",
        Avatar($osoba.map(_.img)).avatar(16)
      ),
      div(
        h1(
          cls := "text-2xl font-bold text-gray-900",
          child.text <-- $osoba.map(_.jmeno)
        ),
        funkce($osoba.map(_.hlavniFunkce)),
        pp($osoba.map(_.pracovniPomer))
      )
    )
  )
