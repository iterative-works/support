package cz.e_bs.cmi.mdr.pdb.app.pages.detail.components

import com.raquo.laminar.api.L.{*, given}
import io.laminext.syntax.core.*
import cz.e_bs.cmi.mdr.pdb.app.components.CustomAttrs
import cz.e_bs.cmi.mdr.pdb.app.components.form.*
import org.scalajs.dom
import com.raquo.laminar.nodes.ReactiveHtmlElement
import cz.e_bs.cmi.mdr.pdb.app.components.files
import cz.e_bs.cmi.mdr.pdb.app.components.files.FilePicker
import cz.e_bs.cmi.mdr.pdb.frontend.AutorizujDukaz
import cz.e_bs.cmi.mdr.pdb.frontend.DocumentRef
import cz.e_bs.cmi.mdr.pdb.app.components.files.File

object UpravDukazForm:
  sealed trait Action
  case object Cancel extends Action
  def apply(onEvent: Observer[Action]): HtmlElement =
    val files = Var[List[File]](Nil)
    def submitButtons: HtmlElement =
      div(
        cls := "pt-5",
        div(
          cls := "flex justify-end",
          button(
            tpe := "button",
            cls := "bg-white py-2 px-4 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500",
            "Zrušit",
            onClick.mapTo(Cancel) --> onEvent
          ),
          button(
            tpe := "submit",
            cls := "disabled:bg-indigo-300 ml-3 inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500",
            disabled <-- files.signal.map(_.isEmpty),
            "Autorizovat"
          )
        )
      )

    div(
      cls := "bg-white shadow px-4 py-5 sm:rounded-lg sm:p-6",
      Form(
        Form.Body(
          Form.Section(
            FormHeader(
              FormHeader.ViewModel(
                "Doložení kritéria",
                "Sestavte doklady poskytující důkaz kritéria a potvrďte odesláním formuláře. Případné limitace či jiné relevantní údaje vepište do pole Komentář."
              )
            ),
            FormFields(
              FormRow(
                "dokumenty",
                "Dokumenty",
                FilePicker(files)
                  .amend(idAttr := "dokumenty", cls("max-w-lg"))
              ).toHtml,
              FormRow(
                "platnost",
                "Platnost",
                input(
                  idAttr := "platnost",
                  name := "platnost",
                  tpe := "date",
                  autoComplete := "date",
                  cls := "block max-w-lg w-full shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm border-gray-300 rounded-md"
                )
              ).toHtml,
              FormRow(
                "komentar",
                "Komentář",
                div(
                  cls := "mt-1 sm:mt-0 sm:col-span-2",
                  textArea(
                    idAttr := "komentar",
                    name := "about",
                    rows := 3,
                    cls := "max-w-lg shadow-sm block w-full focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm border border-gray-300 rounded-md"
                  ),
                  p(
                    cls := "mt-2 text-sm text-gray-500",
                    "Doplňte prosím potřebné informace související s doložením kritéria, včetně případných limitací."
                  )
                )
              ).toHtml
            )
          )
        ),
        submitButtons
      )
    )