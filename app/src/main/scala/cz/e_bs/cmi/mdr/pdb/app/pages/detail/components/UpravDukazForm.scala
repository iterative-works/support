package cz.e_bs.cmi.mdr.pdb.app.pages.detail.components

import com.raquo.laminar.api.L.{*, given}
import cz.e_bs.cmi.mdr.pdb.app.components.CustomAttrs

object UpravDukazForm:
  def apply(): HtmlElement =
    div(
      cls := "bg-white shadow px-4 py-5 sm:rounded-lg sm:p-6",
      form(
        cls := "space-y-8 divide-y divide-gray-200",
        div(
          cls := "space-y-8 divide-y divide-gray-200 sm:space-y-5",
          div(
            div(
              h3(
                cls := "text-lg leading-6 font-medium text-gray-900",
                """Profile"""
              ),
              p(
                cls := "mt-1 max-w-2xl text-sm text-gray-500",
                """This information will be displayed publicly so be careful what you share."""
              )
            ),
            div(
              cls := "mt-6 sm:mt-5 space-y-6 sm:space-y-5",
              div(
                cls := "sm:grid sm:grid-cols-3 sm:gap-4 sm:items-start sm:border-t sm:border-gray-200 sm:pt-5",
                label(
                  forId := "username",
                  cls := "block text-sm font-medium text-gray-700 sm:mt-px sm:pt-2",
                  """Username"""
                ),
                div(
                  cls := "mt-1 sm:mt-0 sm:col-span-2",
                  div(
                    cls := "max-w-lg flex rounded-md shadow-sm",
                    span(
                      cls := "inline-flex items-center px-3 rounded-l-md border border-r-0 border-gray-300 bg-gray-50 text-gray-500 sm:text-sm",
                      """workcation.com/"""
                    ),
                    input(
                      tpe := "text",
                      name := "username",
                      idAttr := "username",
                      autoComplete := "username",
                      cls := "flex-1 block w-full focus:ring-indigo-500 focus:border-indigo-500 min-w-0 rounded-none rounded-r-md sm:text-sm border-gray-300"
                    )
                  )
                )
              ),
              div(
                cls := "sm:grid sm:grid-cols-3 sm:gap-4 sm:items-start sm:border-t sm:border-gray-200 sm:pt-5",
                label(
                  forId := "about",
                  cls := "block text-sm font-medium text-gray-700 sm:mt-px sm:pt-2",
                  """About"""
                ),
                div(
                  cls := "mt-1 sm:mt-0 sm:col-span-2",
                  textArea(
                    idAttr := "about",
                    name := "about",
                    rows := 3,
                    cls := "max-w-lg shadow-sm block w-full focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm border border-gray-300 rounded-md"
                  ),
                  p(
                    cls := "mt-2 text-sm text-gray-500",
                    """Write a few sentences about yourself."""
                  )
                )
              ),
              div(
                cls := "sm:grid sm:grid-cols-3 sm:gap-4 sm:items-center sm:border-t sm:border-gray-200 sm:pt-5",
                label(
                  forId := "photo",
                  cls := "block text-sm font-medium text-gray-700",
                  """Photo"""
                ),
                div(
                  cls := "mt-1 sm:mt-0 sm:col-span-2",
                  div(
                    cls := "flex items-center",
                    span(
                      cls := "h-12 w-12 rounded-full overflow-hidden bg-gray-100", {
                        import svg._
                        svg(
                          cls := "h-full w-full text-gray-300",
                          fill := "currentColor",
                          viewBox := "0 0 24 24",
                          path(
                            d := "M24 20.993V24H0v-2.996A14.977 14.977 0 0112.004 15c4.904 0 9.26 2.354 11.996 5.993zM16.002 8.999a4 4 0 11-8 0 4 4 0 018 0z"
                          )
                        )
                      }
                    ),
                    button(
                      tpe := "button",
                      cls := "ml-5 bg-white py-2 px-3 border border-gray-300 rounded-md shadow-sm text-sm leading-4 font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500",
                      """Change"""
                    )
                  )
                )
              ),
              div(
                cls := "sm:grid sm:grid-cols-3 sm:gap-4 sm:items-start sm:border-t sm:border-gray-200 sm:pt-5",
                label(
                  forId := "cover-photo",
                  cls := "block text-sm font-medium text-gray-700 sm:mt-px sm:pt-2",
                  """Cover photo"""
                ),
                div(
                  cls := "mt-1 sm:mt-0 sm:col-span-2",
                  div(
                    cls := "max-w-lg flex justify-center px-6 pt-5 pb-6 border-2 border-gray-300 border-dashed rounded-md",
                    div(
                      cls := "space-y-1 text-center", {
                        import svg.*
                        svg(
                          cls := "mx-auto h-12 w-12 text-gray-400",
                          stroke := "currentColor",
                          fill := "none",
                          viewBox := "0 0 48 48",
                          CustomAttrs.svg.ariaHidden := true,
                          path(
                            d := "M28 8H12a4 4 0 00-4 4v20m32-12v8m0 0v8a4 4 0 01-4 4H12a4 4 0 01-4-4v-4m32-4l-3.172-3.172a4 4 0 00-5.656 0L28 28M8 32l9.172-9.172a4 4 0 015.656 0L28 28m0 0l4 4m4-24h8m-4-4v8m-12 4h.02",
                            strokeWidth := "2",
                            strokeLineCap := "round",
                            strokeLineJoin := "round"
                          )
                        )
                      },
                      div(
                        cls := "flex text-sm text-gray-600",
                        label(
                          forId := "file-upload",
                          cls := "relative cursor-pointer bg-white rounded-md font-medium text-indigo-600 hover:text-indigo-500 focus-within:outline-none focus-within:ring-2 focus-within:ring-offset-2 focus-within:ring-indigo-500",
                          span(
                            """Upload a file"""
                          ),
                          input(
                            idAttr := "file-upload",
                            name := "file-upload",
                            tpe := "file",
                            cls := "sr-only"
                          )
                        ),
                        p(
                          cls := "pl-1",
                          """or drag and drop"""
                        )
                      ),
                      p(
                        cls := "text-xs text-gray-500",
                        """PNG, JPG, GIF up to 10MB"""
                      )
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
      )
    )
