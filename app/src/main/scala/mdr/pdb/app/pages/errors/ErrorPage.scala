package mdr.pdb.app.pages.errors

import com.raquo.domtypes.generic.codecs.StringAsIsCodec
import com.raquo.laminar.api.L.{*, given}
import mdr.pdb.app.components.PageLink
import mdr.pdb.app.Page
import mdr.pdb.app.Action
import com.raquo.waypoint.Router

object ErrorPage:
  case class ViewModel(
      homePage: Page,
      errorName: String,
      title: String,
      subTitle: String
  )

  def apply(m: ViewModel, actionBus: Observer[Action])(using
      Router[Page]
  ): HtmlElement =
    val ViewModel(homePage, errorName, title, subTitle) = m
    div(
      cls := "min-h-full pt-16 pb-12 flex flex-col bg-white",
      main(
        cls := "flex-grow flex flex-col justify-center max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8",
        div(
          cls := "flex-shrink-0 flex justify-center",
          a(
            href := "/",
            cls := "inline-flex",
            span(
              cls := "sr-only",
              """Workflow"""
            ),
            img(
              cls := "h-12 w-auto",
              src := "https://tailwindui.com/img/logos/workflow-mark.svg?color=indigo&shade=600",
              alt := ""
            )
          )
        ),
        div(
          cls := "py-16",
          div(
            cls := "text-center",
            p(
              cls := "text-sm font-semibold text-indigo-600 uppercase tracking-wide",
              errorName
            ),
            h1(
              cls := "mt-2 text-4xl font-extrabold text-gray-900 tracking-tight sm:text-5xl",
              title
            ),
            p(
              cls := "mt-2 text-base text-gray-500",
              subTitle
            ),
            div(
              cls := "mt-6",
              PageLink
                .container(homePage, actionBus)
                .amend(
                  cls := "text-base font-medium text-indigo-600 hover:text-indigo-500",
                  """Go back home"""
                )
            )
          )
        )
      ),
      footer(
        cls := "flex-shrink-0 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8",
        nav(
          cls := "flex justify-center space-x-4",
          a(
            href := "#",
            cls := "text-sm font-medium text-gray-500 hover:text-gray-600",
            """Contact Support"""
          ),
          span(
            cls := "inline-block border-l border-gray-300",
            aria.hidden := true
          ),
          a(
            href := "#",
            cls := "text-sm font-medium text-gray-500 hover:text-gray-600",
            """Status"""
          ),
          span(
            cls := "inline-block border-l border-gray-300",
            aria.hidden := true
          ),
          a(
            href := "#",
            cls := "text-sm font-medium text-gray-500 hover:text-gray-600",
            """Twitter"""
          )
        )
      )
    )
