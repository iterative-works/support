package mdr.pdb.app.pages.detail.components

import com.raquo.laminar.api.L.{*, given}
import mdr.pdb.OsobniCislo

import java.time.LocalDate
import works.iterative.ui.components.tailwind.Avatar

object DetailOsoby:

  object Funkce:
    case class ViewModel(
        nazev: String,
        stredisko: String,
        voj: String
    )

    def render($m: Signal[ViewModel]): HtmlElement =
      p(
        cls := "text-sm font-medium text-gray-500",
        child.text <-- $m.map(_.nazev),
        span(
          cls := "hidden md:inline",
          " @ ",
          child.text <-- $m.map(_.stredisko),
          ", ",
          child.text <-- $m.map(_.voj)
        )
      )

  object PracovniPomer:
    case class ViewModel(
        druh: String,
        pocatek: LocalDate,
        konec: Option[LocalDate]
    )

    def render($m: Signal[ViewModel]): HtmlElement =
      import works.iterative.ui.components.tailwind.CustomAttrs.datetime
      p(
        cls := "text-sm font-medium text-gray-500",
        child.text <-- $m.map(_.druh),
        " od ",
        time(
          datetime <-- $m.map(_.pocatek.toString),
          child.text <-- $m.map(_.pocatek.toString)
        )
      )

  case class ViewModel(
      osobniCislo: OsobniCislo,
      jmeno: String,
      email: Option[String],
      telefon: Option[String],
      img: Option[String],
      hlavniFunkce: Option[Funkce.ViewModel],
      pracovniPomer: Option[PracovniPomer.ViewModel]
  )

  def apply($m: Signal[ViewModel]): HtmlElement =
    div(
      cls := "md:flex md:items-center md:justify-between md:space-x-4",
      div(
        cls := "flex items-start space-x-4",
        div(
          cls := "flex-shrink-0",
          Avatar($m.map(_.img)).avatar(16)
        ),
        div(
          h1(
            cls := "text-2xl font-bold text-gray-900",
            child.text <-- $m.map(_.jmeno)
          ),
          child.maybe <-- $m.map(_.hlavniFunkce).split(_ => ())((_, _, d) =>
            Funkce.render(d)
          ),
          child.maybe <-- $m.map(_.pracovniPomer).split(_ => ())((_, _, d) =>
            PracovniPomer.render(d)
          )
        )
      )
    )

  def header($m: Signal[ViewModel]): HtmlElement =
    div(
      cls := "md:flex md:items-center md:justify-between md:space-x-5",
      div(
        cls := "flex items-start space-x-4",
        div(
          cls := "flex-shrink-0",
          Avatar($m.map(_.img)).avatar(8)
        ),
        div(
          h1(
            cls := "text-lg font-medium text-gray-800",
            child.text <-- $m.map(_.jmeno)
          )
        )
      )
    )
