package cz.e_bs.cmi.mdr.pdb.app.pages.detail.components

import com.raquo.laminar.api.L.{*, given}
import java.time.LocalDate
import cz.e_bs.cmi.mdr.pdb.app.components.CustomAttrs

object DukazKriteria:
  case class Osoba(osobniCislo: String, jmeno: String)
  case class Udalost(osoba: Osoba, datum: LocalDate)
  case class Dokument(
      url: String,
      nazev: String,
      pridal: Udalost,
      odebral: Option[Udalost]
  )
  case class ViewModel(
      dokumenty: List[Dokument],
      autorizoval: Option[Udalost],
      platiDo: Option[LocalDate],
      poznámka: Option[String]
  )
  def apply($m: Signal[ViewModel]): HtmlElement =
    div(
      cls := "bg-white shadow overflow-hidden sm:rounded-lg",
      div(
        cls := "px-4 py-5 sm:px-6",
        h3(
          cls := "text-lg leading-6 font-medium text-gray-900",
          """Applicant Information"""
        ),
        p(
          cls := "mt-1 max-w-2xl text-sm text-gray-500",
          """Personal details and application."""
        )
      ),
      div(
        cls := "border-t border-gray-200 px-4 py-5 sm:px-6",
        dl(
          cls := "grid grid-cols-1 gap-x-4 gap-y-8 sm:grid-cols-2",
          div(
            cls := "sm:col-span-1",
            dt(
              cls := "text-sm font-medium text-gray-500",
              """Full name"""
            ),
            dd(
              cls := "mt-1 text-sm text-gray-900",
              """Margot Foster"""
            )
          ),
          div(
            cls := "sm:col-span-1",
            dt(
              cls := "text-sm font-medium text-gray-500",
              """Application for"""
            ),
            dd(
              cls := "mt-1 text-sm text-gray-900",
              """Backend Developer"""
            )
          ),
          div(
            cls := "sm:col-span-1",
            dt(
              cls := "text-sm font-medium text-gray-500",
              """Email address"""
            ),
            dd(
              cls := "mt-1 text-sm text-gray-900",
              """margotfoster@example.com"""
            )
          ),
          div(
            cls := "sm:col-span-1",
            dt(
              cls := "text-sm font-medium text-gray-500",
              """Salary expectation"""
            ),
            dd(
              cls := "mt-1 text-sm text-gray-900",
              """$120,000"""
            )
          ),
          div(
            cls := "sm:col-span-2",
            dt(
              cls := "text-sm font-medium text-gray-500",
              """About"""
            ),
            dd(
              cls := "mt-1 text-sm text-gray-900",
              """Fugiat ipsum ipsum deserunt culpa aute sint do nostrud anim incididunt cillum culpa consequat. Excepteur qui ipsum aliquip consequat sint. Sit id mollit nulla mollit nostrud in ea officia proident. Irure nostrud pariatur mollit ad adipisicing reprehenderit deserunt qui eu."""
            )
          ),
          div(
            cls := "sm:col-span-2",
            dt(
              cls := "text-sm font-medium text-gray-500",
              """Attachments"""
            ),
            dd(
              cls := "mt-1 text-sm text-gray-900",
              ul(
                role := "list",
                cls := "border border-gray-200 rounded-md divide-y divide-gray-200",
                li(
                  cls := "pl-3 pr-4 py-3 flex items-center justify-between text-sm",
                  div(
                    cls := "w-0 flex-1 flex items-center", {
                      import svg.*
                      import CustomAttrs.svg.ariaHidden
                      svg(
                        cls := "flex-shrink-0 h-5 w-5 text-gray-400",
                        xmlns := "http://www.w3.org/2000/svg",
                        viewBox := "0 0 20 20",
                        fill := "currentColor",
                        ariaHidden := true,
                        path(
                          fillRule := "evenodd",
                          d := "M8 4a3 3 0 00-3 3v4a5 5 0 0010 0V7a1 1 0 112 0v4a7 7 0 11-14 0V7a5 5 0 0110 0v4a3 3 0 11-6 0V7a1 1 0 012 0v4a1 1 0 102 0V7a3 3 0 00-3-3z",
                          clipRule := "evenodd"
                        )
                      )
                    },
                    span(
                      cls := "ml-2 flex-1 w-0 truncate",
                      """resume_back_end_developer.pdf"""
                    )
                  ),
                  div(
                    cls := "ml-4 flex-shrink-0",
                    a(
                      href := "#",
                      cls := "font-medium text-indigo-600 hover:text-indigo-500",
                      """Download"""
                    )
                  )
                ),
                li(
                  cls := "pl-3 pr-4 py-3 flex items-center justify-between text-sm",
                  div(
                    cls := "w-0 flex-1 flex items-center", {
                      import svg.*
                      import CustomAttrs.svg.ariaHidden
                      svg(
                        cls := "flex-shrink-0 h-5 w-5 text-gray-400",
                        xmlns := "http://www.w3.org/2000/svg",
                        viewBox := "0 0 20 20",
                        fill := "currentColor",
                        ariaHidden := true,
                        path(
                          fillRule := "evenodd",
                          d := "M8 4a3 3 0 00-3 3v4a5 5 0 0010 0V7a1 1 0 112 0v4a7 7 0 11-14 0V7a5 5 0 0110 0v4a3 3 0 11-6 0V7a1 1 0 012 0v4a1 1 0 102 0V7a3 3 0 00-3-3z",
                          clipRule := "evenodd"
                        )
                      )
                    },
                    span(
                      cls := "ml-2 flex-1 w-0 truncate",
                      """coverletter_back_end_developer.pdf"""
                    )
                  ),
                  div(
                    cls := "ml-4 flex-shrink-0",
                    a(
                      href := "#",
                      cls := "font-medium text-indigo-600 hover:text-indigo-500",
                      """Download"""
                    )
                  )
                )
              )
            )
          )
        )
      )
    )
