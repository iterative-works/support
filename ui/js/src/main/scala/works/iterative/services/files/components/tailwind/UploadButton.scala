package services.files.components.tailwind

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import works.iterative.ui.UIString
import org.scalajs.dom.FileList
import works.iterative.ui.components.tailwind.HtmlComponent

case class UploadButton(title: UIString)

object UploadButton:
  class Component(upload: Observer[FileList])
      extends HtmlComponent[org.scalajs.dom.html.Div, UploadButton]:
    override def render(u: UploadButton) =
      div(
        cls := "mt-4 sm:mt-0 sm:ml-16 sm:flex-none",
        label(
          cls("block w-full"),
          div(
            cls(
              "inline-flex items-center justify-center rounded-md border border-transparent bg-indigo-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 sm:w-auto"
            ), {
              import svg.*
              svg(
                cls("w-6 h-6"),
                fill := "#FFF",
                viewBox := "0 0 24 24",
                xmlns := "http://www.w3.org/2000/svg",
                path(d := "M0 0h24v24H0z", fill := "none"),
                path(d := "M9 16h6v-6h4l-7-7-7 7h4zm-4 2h14v2H5z")
              )
            },
            span(cls := "ml-2", u.title)
          ),
          input(
            cls := "cursor-pointer hidden",
            tpe := "file",
            nameAttr := "files",
            multiple := true,
            inContext(thisNode => onInput.mapTo(thisNode.ref.files) --> upload)
          )
        )
      )
