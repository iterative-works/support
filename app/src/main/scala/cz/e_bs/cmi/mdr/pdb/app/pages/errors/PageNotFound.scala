package cz.e_bs.cmi.mdr.pdb.app.pages.errors

import com.raquo.domtypes.generic.codecs.StringAsIsCodec
import com.raquo.laminar.api.L.{*, given}

def PageNotFound(url: String, basePath: String): HtmlElement =
  div(
    cls := "bg-white min-h-full flex flex-col lg:relative",
    div(
      cls := "flex-grow flex flex-col",
      main(
        cls := "flex-grow flex flex-col bg-white",
        div(
          cls := "flex-grow mx-auto max-w-7xl w-full flex flex-col px-4 sm:px-6 lg:px-8",
          div(
            cls := "flex-shrink-0 pt-10 sm:pt-16",
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
            cls := "flex-shrink-0 my-auto py-16 sm:py-32",
            p(
              cls := "text-sm font-semibold text-indigo-600 uppercase tracking-wide",
              """404 error"""
            ),
            h1(
              cls := "mt-2 text-4xl font-extrabold text-gray-900 tracking-tight sm:text-5xl",
              s"Page not found"
            ),
            p(
              cls := "mt-2 text-base text-gray-500",
              s"Sorry, but page ${url} doesn't exist."
            ),
            div(
              cls := "mt-6",
              a(
                href := basePath,
                cls := "text-base font-medium text-indigo-600 hover:text-indigo-500",
                """Go back home"""
              )
            )
          )
        )
      ),
      footer(
        cls := "flex-shrink-0 bg-gray-50",
        div(
          cls := "mx-auto max-w-7xl w-full px-4 py-16 sm:px-6 lg:px-8",
          nav(
            cls := "flex space-x-4",
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
    ),
    div(
      cls := "hidden lg:block lg:absolute lg:inset-y-0 lg:right-0 lg:w-1/2",
      img(
        cls := "absolute inset-0 h-full w-full object-cover",
        src := "https://images.unsplash.com/photo-1470847355775-e0e3c35a9a2c?ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&ixlib=rb-1.2.1&auto=format&fit=crop&w=1825&q=80",
        alt := ""
      )
    )
  )
