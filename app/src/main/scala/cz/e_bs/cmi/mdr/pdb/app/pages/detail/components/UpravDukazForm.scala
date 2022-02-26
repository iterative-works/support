package cz.e_bs.cmi.mdr.pdb.app.pages.detail.components

import com.raquo.laminar.api.L.{*, given}
import io.laminext.syntax.core.*
import cz.e_bs.cmi.mdr.pdb.app.components.CustomAttrs
import org.scalajs.dom
import com.raquo.laminar.nodes.ReactiveHtmlElement

object UpravDukazForm:
  import com.raquo.laminar.api.L.{*, given}

  object FormHeader:
    case class ViewModel(header: String, description: String)
    def apply(m: ViewModel): HtmlElement =
      div(
        h3(cls := "text-lg leading-6 font-medium text-gray-900", m.header),
        p(cls := "mt-1 max-w-2xl text-sm text-gray-500", m.description)
      )

  object FormSection:
    def apply(
        mods: Modifier[ReactiveHtmlElement[dom.HTMLElement]]*
    ): HtmlElement =
      div(
        cls := "mt-6 sm:mt-5 space-y-6 sm:space-y-5",
        mods
      )

  object FormRow:
    case class ViewModel(id: String, label: String, content: HtmlElement)
    def apply(m: ViewModel): HtmlElement =
      div(
        cls := "sm:grid sm:grid-cols-3 sm:gap-4 sm:items-start sm:border-t sm:border-gray-200 sm:pt-5",
        label(
          forId := m.id,
          cls := "block text-sm font-medium text-gray-700 sm:mt-px sm:pt-2",
          m.label
        ),
        div(
          cls := "mt-1 sm:mt-0 sm:col-span-2",
          m.content
        )
      )

  object ComboBox:
    object ComboOption:
      case class ViewModel(value: String, active: Boolean)
      def apply(m: ViewModel): HtmlElement =
        li(
          cls := "relative cursor-default select-none py-2 pl-8 pr-4",
          cls := (if m.active then "text-white bg-indigo-600"
                  else "text-gray-900"),
          idAttr := "option-0",
          role := "option",
          tabIndex := -1,
          span(
            cls := "block truncate",
            m.value
          ),
          if m.active then
            span(
              cls := "absolute inset-y-0 left-0 flex items-center pl-1.5",
              cls := (if m.active then "text-white" else "text-indigo-600"), {
                import svg.*
                svg(
                  cls := "h-5 w-5",
                  xmlns := "http://www.w3.org/2000/svg",
                  viewBox := "0 0 20 20",
                  fill := "currentColor",
                  CustomAttrs.svg.ariaHidden := true,
                  path(
                    fillRule := "evenodd",
                    d := "M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z",
                    clipRule := "evenodd"
                  )
                )
              }
            )
          else emptyNode
        )
    case class ViewModel(id: String, options: List[ComboOption.ViewModel])
    def apply(m: ViewModel): HtmlElement =
      val isOpen = Var(false)
      div(
        cls := "relative mt-1",
        input(
          idAttr := m.id,
          tpe := "text",
          cls := "w-full rounded-md border border-gray-300 bg-white py-2 pl-3 pr-12 shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500 sm:text-sm",
          role := "combobox",
          aria.controls := "options",
          aria.expanded := false,
          onClick.mapTo(true) --> isOpen.writer
        ),
        button(
          tpe := "button",
          cls := "absolute inset-y-0 right-0 flex items-center rounded-r-md px-2 focus:outline-none", {
            import svg.*
            svg(
              cls := "h-5 w-5 text-gray-400",
              xmlns := "http://www.w3.org/2000/svg",
              viewBox := "0 0 20 20",
              fill := "currentColor",
              CustomAttrs.svg.ariaHidden := true,
              path(
                fillRule := "evenodd",
                d := "M10 3a1 1 0 01.707.293l3 3a1 1 0 01-1.414 1.414L10 5.414 7.707 7.707a1 1 0 01-1.414-1.414l3-3A1 1 0 0110 3zm-3.707 9.293a1 1 0 011.414 0L10 14.586l2.293-2.293a1 1 0 011.414 1.414l-3 3a1 1 0 01-1.414 0l-3-3a1 1 0 010-1.414z",
                clipRule := "evenodd"
              )
            )
          }
        ),
        ul(
          cls <-- isOpen.signal.switch("", "hidden"),
          cls := "absolute z-10 mt-1 max-h-60 w-full overflow-auto rounded-md bg-white py-1 text-base shadow-lg ring-1 ring-black ring-opacity-5 focus:outline-none sm:text-sm",
          idAttr := "options",
          role := "listbox",
          m.options.map(ComboOption(_))
        )
      )

  object SubmitButtons:
    def apply: HtmlElement =
      div(
        cls := "pt-5",
        div(
          cls := "flex justify-end",
          button(
            tpe := "button",
            cls := "bg-white py-2 px-4 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500",
            """Cancel"""
          ),
          button(
            tpe := "submit",
            cls := "ml-3 inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500",
            """Save"""
          )
        )
      )

  def apply(): HtmlElement =
    div(
      cls := "bg-white shadow px-4 py-5 sm:rounded-lg sm:p-6",
      form(
        cls := "space-y-8 divide-y divide-gray-200",
        div(
          cls := "space-y-8 divide-y divide-gray-200 sm:space-y-5",
          div(
            FormHeader(
              FormHeader.ViewModel(
                "Doložení kritéria",
                "Sestavte doklady poskytující důkaz kritéria a potvrďte odesláním formuláře. Případné limitace či jiné relevantní údaje vepište do pole Komentář."
              )
            ),
            FormSection(
              FormRow(
                FormRow.ViewModel(
                  "dokumenty",
                  "Dokumenty",
                  ComboBox(
                    ComboBox.ViewModel(
                      "dokumenty",
                      List(
                        ComboBox.ComboOption.ViewModel(
                          "Nebyly nalezeny žádné dokumenty.",
                          false
                        )
                      )
                    )
                  )
                )
              ),
              FormRow(
                FormRow.ViewModel(
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
                      "Doplňte prosímpotřebné informace související s doložením kritéria, včetně případných limitací."
                    )
                  )
                )
              )
            )
          )
          /*
        div(
          cls := "pt-8 space-y-6 sm:pt-10 sm:space-y-5",
          div(
            h3(
              cls := "text-lg leading-6 font-medium text-gray-900",
              """Personal Information"""
            ),
            p(
              cls := "mt-1 max-w-2xl text-sm text-gray-500",
              """Use a permanent address where you can receive mail."""
            )
          ),
          div(
            cls := "space-y-6 sm:space-y-5",
            div(
              cls := "sm:grid sm:grid-cols-3 sm:gap-4 sm:items-start sm:border-t sm:border-gray-200 sm:pt-5",
              label(
                forId := "first-name",
                cls := "block text-sm font-medium text-gray-700 sm:mt-px sm:pt-2",
                """First name"""
              ),
              div(
                cls := "mt-1 sm:mt-0 sm:col-span-2",
                input(
                  tpe := "text",
                  name := "first-name",
                  idAttr := "first-name",
                  autocomplete := "given-name",
                  cls := "max-w-lg block w-full shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:max-w-xs sm:text-sm border-gray-300 rounded-md"
                )
              )
            ),
            div(
              cls := "sm:grid sm:grid-cols-3 sm:gap-4 sm:items-start sm:border-t sm:border-gray-200 sm:pt-5",
              label(
                forId := "last-name",
                cls := "block text-sm font-medium text-gray-700 sm:mt-px sm:pt-2",
                """Last name"""
              ),
              div(
                cls := "mt-1 sm:mt-0 sm:col-span-2",
                input(
                  tpe := "text",
                  name := "last-name",
                  idAttr := "last-name",
                  autocomplete := "family-name",
                  cls := "max-w-lg block w-full shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:max-w-xs sm:text-sm border-gray-300 rounded-md"
                )
              )
            ),
            div(
              cls := "sm:grid sm:grid-cols-3 sm:gap-4 sm:items-start sm:border-t sm:border-gray-200 sm:pt-5",
              label(
                forId := "email",
                cls := "block text-sm font-medium text-gray-700 sm:mt-px sm:pt-2",
                """Email address"""
              ),
              div(
                cls := "mt-1 sm:mt-0 sm:col-span-2",
                input(
                  idAttr := "email",
                  name := "email",
                  tpe := "email",
                  autocomplete := "email",
                  cls := "block max-w-lg w-full shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm border-gray-300 rounded-md"
                )
              )
            ),
            div(
              cls := "sm:grid sm:grid-cols-3 sm:gap-4 sm:items-start sm:border-t sm:border-gray-200 sm:pt-5",
              label(
                forId := "country",
                cls := "block text-sm font-medium text-gray-700 sm:mt-px sm:pt-2",
                """Country"""
              ),
              div(
                cls := "mt-1 sm:mt-0 sm:col-span-2",
                select(
                  idAttr := "country",
                  name := "country",
                  autocomplete := "country-name",
                  cls := "max-w-lg block focus:ring-indigo-500 focus:border-indigo-500 w-full shadow-sm sm:max-w-xs sm:text-sm border-gray-300 rounded-md",
                  option(
                    """United States"""
                  ),
                  option(
                    """Canada"""
                  ),
                  option(
                    """Mexico"""
                  )
                )
              )
            ),
            div(
              cls := "sm:grid sm:grid-cols-3 sm:gap-4 sm:items-start sm:border-t sm:border-gray-200 sm:pt-5",
              label(
                forId := "street-address",
                cls := "block text-sm font-medium text-gray-700 sm:mt-px sm:pt-2",
                """Street address"""
              ),
              div(
                cls := "mt-1 sm:mt-0 sm:col-span-2",
                input(
                  tpe := "text",
                  name := "street-address",
                  idAttr := "street-address",
                  autocomplete := "street-address",
                  cls := "block max-w-lg w-full shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm border-gray-300 rounded-md"
                )
              )
            ),
            div(
              cls := "sm:grid sm:grid-cols-3 sm:gap-4 sm:items-start sm:border-t sm:border-gray-200 sm:pt-5",
              label(
                forId := "city",
                cls := "block text-sm font-medium text-gray-700 sm:mt-px sm:pt-2",
                """City"""
              ),
              div(
                cls := "mt-1 sm:mt-0 sm:col-span-2",
                input(
                  tpe := "text",
                  name := "city",
                  idAttr := "city",
                  autocomplete := "address-level2",
                  cls := "max-w-lg block w-full shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:max-w-xs sm:text-sm border-gray-300 rounded-md"
                )
              )
            ),
            div(
              cls := "sm:grid sm:grid-cols-3 sm:gap-4 sm:items-start sm:border-t sm:border-gray-200 sm:pt-5",
              label(
                forId := "region",
                cls := "block text-sm font-medium text-gray-700 sm:mt-px sm:pt-2",
                """State / Province"""
              ),
              div(
                cls := "mt-1 sm:mt-0 sm:col-span-2",
                input(
                  tpe := "text",
                  name := "region",
                  idAttr := "region",
                  autocomplete := "address-level1",
                  cls := "max-w-lg block w-full shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:max-w-xs sm:text-sm border-gray-300 rounded-md"
                )
              )
            ),
            div(
              cls := "sm:grid sm:grid-cols-3 sm:gap-4 sm:items-start sm:border-t sm:border-gray-200 sm:pt-5",
              label(
                forId := "postal-code",
                cls := "block text-sm font-medium text-gray-700 sm:mt-px sm:pt-2",
                """ZIP / Postal code"""
              ),
              div(
                cls := "mt-1 sm:mt-0 sm:col-span-2",
                input(
                  tpe := "text",
                  name := "postal-code",
                  idAttr := "postal-code",
                  autocomplete := "postal-code",
                  cls := "max-w-lg block w-full shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:max-w-xs sm:text-sm border-gray-300 rounded-md"
                )
              )
            )
          )
        ),
        div(
          cls := "divide-y divide-gray-200 pt-8 space-y-6 sm:pt-10 sm:space-y-5",
          div(
            h3(
              cls := "text-lg leading-6 font-medium text-gray-900",
              """Notifications"""
            ),
            p(
              cls := "mt-1 max-w-2xl text-sm text-gray-500",
              """We'll always let you know about important changes, but you pick what else you want to hear about."""
            )
          ),
          div(
            cls := "space-y-6 sm:space-y-5 divide-y divide-gray-200",
            div(
              cls := "pt-6 sm:pt-5",
              div(
                role := "group",
                aria.labelledby := "label-email",
                div(
                  cls := "sm:grid sm:grid-cols-3 sm:gap-4 sm:items-baseline",
                  div(
                    div(
                      cls := "text-base font-medium text-gray-900 sm:text-sm sm:text-gray-700",
                      idAttr := "label-email",
                      """By Email"""
                    )
                  ),
                  div(
                    cls := "mt-4 sm:mt-0 sm:col-span-2",
                    div(
                      cls := "max-w-lg space-y-4",
                      div(
                        cls := "relative flex items-start",
                        div(
                          cls := "flex items-center h-5",
                          input(
                            idAttr := "comments",
                            name := "comments",
                            tpe := "checkbox",
                            cls := "focus:ring-indigo-500 h-4 w-4 text-indigo-600 border-gray-300 rounded"
                          )
                        ),
                        div(
                          cls := "ml-3 text-sm",
                          label(
                            forId := "comments",
                            cls := "font-medium text-gray-700",
                            """Comments"""
                          ),
                          p(
                            cls := "text-gray-500",
                            """Get notified when someones posts a comment on a posting."""
                          )
                        )
                      ),
                      div(
                        div(
                          cls := "relative flex items-start",
                          div(
                            cls := "flex items-center h-5",
                            input(
                              idAttr := "candidates",
                              name := "candidates",
                              tpe := "checkbox",
                              cls := "focus:ring-indigo-500 h-4 w-4 text-indigo-600 border-gray-300 rounded"
                            )
                          ),
                          div(
                            cls := "ml-3 text-sm",
                            label(
                              forId := "candidates",
                              cls := "font-medium text-gray-700",
                              """Candidates"""
                            ),
                            p(
                              cls := "text-gray-500",
                              """Get notified when a candidate applies for a job."""
                            )
                          )
                        )
                      ),
                      div(
                        div(
                          cls := "relative flex items-start",
                          div(
                            cls := "flex items-center h-5",
                            input(
                              idAttr := "offers",
                              name := "offers",
                              tpe := "checkbox",
                              cls := "focus:ring-indigo-500 h-4 w-4 text-indigo-600 border-gray-300 rounded"
                            )
                          ),
                          div(
                            cls := "ml-3 text-sm",
                            label(
                              forId := "offers",
                              cls := "font-medium text-gray-700",
                              """Offers"""
                            ),
                            p(
                              cls := "text-gray-500",
                              """Get notified when a candidate accepts or rejects an offer."""
                            )
                          )
                        )
                      )
                    )
                  )
                )
              )
            ),
            div(
              cls := "pt-6 sm:pt-5",
              div(
                role := "group",
                aria.labelledby := "label-notifications",
                div(
                  cls := "sm:grid sm:grid-cols-3 sm:gap-4 sm:items-baseline",
                  div(
                    div(
                      cls := "text-base font-medium text-gray-900 sm:text-sm sm:text-gray-700",
                      idAttr := "label-notifications",
                      """Push Notifications"""
                    )
                  ),
                  div(
                    cls := "sm:col-span-2",
                    div(
                      cls := "max-w-lg",
                      p(
                        cls := "text-sm text-gray-500",
                        """These are delivered via SMS to your mobile phone."""
                      ),
                      div(
                        cls := "mt-4 space-y-4",
                        div(
                          cls := "flex items-center",
                          input(
                            idAttr := "push-everything",
                            name := "push-notifications",
                            tpe := "radio",
                            cls := "focus:ring-indigo-500 h-4 w-4 text-indigo-600 border-gray-300"
                          ),
                          label(
                            forId := "push-everything",
                            cls := "ml-3 block text-sm font-medium text-gray-700",
                            """Everything"""
                          )
                        ),
                        div(
                          cls := "flex items-center",
                          input(
                            idAttr := "push-email",
                            name := "push-notifications",
                            tpe := "radio",
                            cls := "focus:ring-indigo-500 h-4 w-4 text-indigo-600 border-gray-300"
                          ),
                          label(
                            forId := "push-email",
                            cls := "ml-3 block text-sm font-medium text-gray-700",
                            """Same as email"""
                          )
                        ),
                        div(
                          cls := "flex items-center",
                          input(
                            idAttr := "push-nothing",
                            name := "push-notifications",
                            tpe := "radio",
                            cls := "focus:ring-indigo-500 h-4 w-4 text-indigo-600 border-gray-300"
                          ),
                          label(
                            forId := "push-nothing",
                            cls := "ml-3 block text-sm font-medium text-gray-700",
                            """No push notifications"""
                          )
                        )
                      )
                    )
                  )
                )
              )
            )
          )
         )
           */
        ),
        SubmitButtons.apply
      )
    )
