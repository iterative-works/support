package cz.e_bs.cmi.mdr.pdb.app.pages

import com.raquo.laminar.api.L.{*, given}
import com.raquo.domtypes.generic.codecs.StringAsIsCodec

def DetailPage: HtmlElement =
  div(
    cls := "md:flex md:items-center md:justify-between md:space-x-5",
    div(
      cls := "flex items-start space-x-5",
      div(
        cls := "flex-shrink-0",
        div(
          cls := "relative",
          img(
            cls := "h-16 w-16 rounded-full",
            src := "https://images.unsplash.com/photo-1463453091185-61582044d556?ixlib=rb-=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=facearea&facepad=8&w=1024&h=1024&q=80",
            alt := ""
          ),
          span(
            cls := "absolute inset-0 shadow-inner rounded-full",
            aria.hidden := true
          )
        )
      ),
      div(
        cls := "pt-1.5",
        h1(
          cls := "text-2xl font-bold text-gray-900",
          """Ricardo Cooper"""
        ),
        p(
          cls := "text-sm font-medium text-gray-500",
          """Applied for""",
          a(
            href := "#",
            cls := "text-gray-900",
            """Front End Developer"""
          ),
          """on""",
          time(
            customHtmlAttr("datetime", StringAsIsCodec) := "2020-08-25",
            """August 25, 2020"""
          )
        )
      )
    ),
    div(
      cls := "mt-6 flex flex-col-reverse justify-stretch space-y-4 space-y-reverse sm:flex-row-reverse sm:justify-end sm:space-x-reverse sm:space-y-0 sm:space-x-3 md:mt-0 md:flex-row md:space-x-3",
      button(
        tpe := "button",
        cls := "inline-flex items-center justify-center px-4 py-2 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-offset-gray-100 focus:ring-indigo-500",
        """Disqualify"""
      ),
      button(
        tpe := "button",
        cls := "inline-flex items-center justify-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-offset-gray-100 focus:ring-indigo-500",
        """Advance to offer"""
      )
    )
  )
