package cz.e_bs.cmi.mdr.pdb.app

import com.raquo.laminar.api.L.{*, given}
import com.raquo.domtypes.generic.codecs.StringAsIsCodec

object Layout {
  val ariaCurrent = customProp("aria-current", StringAsIsCodec)

  def layout(content: HtmlElement): HtmlElement =
    div(
      cls := "min-h-full",
      nav(
        cls := "bg-indigo-600",
        div(
          cls := "max-w-7xl mx-auto px-4 sm:px-6 lg:px-8",
          div(
            cls := "flex items-center justify-between h-16",
            div(
              cls := "flex items-center",
              div(
                cls := "flex-shrink-0",
                img(
                  cls := "h-8 w-8",
                  src := "https://tailwindui.com/img/logos/workflow-mark-indigo-300.svg",
                  alt := "Workflow"
                )
              ),
              div(
                cls := "hidden md:block",
                div(
                  cls := "ml-10 flex items-baseline space-x-4",
                  // <!-- Current: "bg-indigo-700 text-white", Default: "text-white hover:bg-indigo-500 hover:bg-opacity-75" -->
                  a(
                    href := "#",
                    cls := "bg-indigo-700 text-white px-3 py-2 rounded-md text-sm font-medium",
                    ariaCurrent := "page",
                    "Dashboard"
                  ),
                  a(
                    href := "#",
                    cls := "text-white hover:bg-indigo-500 hover:bg-opacity-75 px-3 py-2 rounded-md text-sm font-medium",
                    "Team"
                  ),
                  a(
                    href := "#",
                    cls := "text-white hover:bg-indigo-500 hover:bg-opacity-75 px-3 py-2 rounded-md text-sm font-medium",
                    "Projects"
                  ),
                  a(
                    href := "#",
                    cls := "text-white hover:bg-indigo-500 hover:bg-opacity-75 px-3 py-2 rounded-md text-sm font-medium",
                    "Calendar"
                  ),
                  a(
                    href := "#",
                    cls := "text-white hover:bg-indigo-500 hover:bg-opacity-75 px-3 py-2 rounded-md text-sm font-medium",
                    "Reports"
                  )
                )
              )
            ),
            div(
              cls := "hidden md:block",
              div(
                cls := "ml-4 flex items-center md:ml-6",
                button(
                  tpe := "button",
                  cls := "p-1 bg-indigo-600 rounded-full text-indigo-200 hover:text-white focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-offset-indigo-600 focus:ring-white",
                  span(cls := "sr-only", "View notifications"),
                  Icons.outline.bell
                ),

                // <!-- Profile dropdown -->
                div(
                  cls := "ml-3 relative",
                  div(
                    button(
                      tpe := "button",
                      cls := "max-w-xs bg-indigo-600 rounded-full flex items-center text-sm focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-offset-indigo-600 focus:ring-white",
                      idAttr := "user-menu-button",
                      aria.expanded := false,
                      aria.hasPopup := true,
                      span(cls := "sr-only", "Open user menu"),
                      img(
                        cls := "h-8 w-8 rounded-full",
                        src := "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=facearea&facepad=2&w=256&h=256&q=80",
                        alt := ""
                      )
                    )
                  ),
                  /*
                   * <!-- Dropdown menu, show/hide based on menu state.

                Entering: "transition ease-out duration-100"
                  From: "transform opacity-0 scale-95"
                  To: "transform opacity-100 scale-100"
                Leaving: "transition ease-in duration-75"
                  From: "transform opacity-100 scale-100"
                  To: "transform opacity-0 scale-95"
              --> */
                  div(
                    cls := "origin-top-right absolute right-0 mt-2 w-48 rounded-md shadow-lg py-1 bg-white ring-1 ring-black ring-opacity-5 focus:outline-none",
                    role := "menu",
                    aria.orientation := "vertical",
                    aria.labelledBy := "user-menu-button",
                    tabIndex := -1,
                    // <!-- Active: "bg-gray-100", Not Active: "" -->
                    a(
                      href := "#",
                      cls := "block px-4 py-2 text-sm text-gray-700",
                      role := "menuitem",
                      tabIndex := -1,
                      idAttr := "user-menu-item-0",
                      "Your Profile"
                    ),
                    a(
                      href := "#",
                      cls := "block px-4 py-2 text-sm text-gray-700",
                      role := "menuitem",
                      tabIndex := -1,
                      idAttr := "user-menu-item-1",
                      "Settings"
                    ),
                    a(
                      href := "#",
                      cls := "block px-4 py-2 text-sm text-gray-700",
                      role := "menuitem",
                      tabIndex := -1,
                      idAttr := "user-menu-item-2",
                      "Sign out"
                    )
                  )
                )
              )
            ),
            div(
              cls := "-mr-2 flex md:hidden",
              // <!-- Mobile menu button -->
              button(
                tpe := "button",
                cls := "bg-indigo-600 inline-flex items-center justify-center p-2 rounded-md text-indigo-200 hover:text-white hover:bg-indigo-500 hover:bg-opacity-75 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-offset-indigo-600 focus:ring-white",
                aria.controls := "mobile-menu",
                aria.expanded := false,
                span(cls := "sr-only", "Open main menu"),
                Icons.outline.menu,
                Icons.outline.x
              )
            )
          )
        ),

        // <!-- Mobile menu, show/hide based on menu state. -->
        div(
          cls := "md:hidden",
          idAttr := "mobile-menu",
          div(
            cls := "px-2 pt-2 pb-3 space-y-1 sm:px-3",
            // <!-- Current: "bg-indigo-700 text-white", Default: "text-white hover:bg-indigo-500 hover:bg-opacity-75" -->
            a(
              href := "#",
              cls := "bg-indigo-700 text-white block px-3 py-2 rounded-md text-base font-medium",
              ariaCurrent := "page",
              "Dashboard"
            ),
            a(
              href := "#",
              cls := "text-white hover:bg-indigo-500 hover:bg-opacity-75 block px-3 py-2 rounded-md text-base font-medium",
              "Team"
            ),
            a(
              href := "#",
              cls := "text-white hover:bg-indigo-500 hover:bg-opacity-75 block px-3 py-2 rounded-md text-base font-medium",
              "Projects"
            ),
            a(
              href := "#",
              cls := "text-white hover:bg-indigo-500 hover:bg-opacity-75 block px-3 py-2 rounded-md text-base font-medium",
              "Calendar"
            ),
            a(
              href := "#",
              cls := "text-white hover:bg-indigo-500 hover:bg-opacity-75 block px-3 py-2 rounded-md text-base font-medium",
              "Reports"
            )
          ),
          div(
            cls := "pt-4 pb-3 border-t border-indigo-700",
            div(
              cls := "flex items-center px-5",
              div(
                cls := "flex-shrink-0",
                img(
                  cls := "h-10 w-10 rounded-full",
                  src := "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=facearea&facepad=2&w=256&h=256&q=80",
                  alt := ""
                )
              ),
              div(
                cls := "ml-3",
                div(cls := "text-base font-medium text-white", "Tom Cook"),
                div(
                  cls := "text-sm font-medium text-indigo-300",
                  "tom@example.com"
                )
              ),
              button(
                tpe := "button",
                cls := "ml-auto bg-indigo-600 flex-shrink-0 p-1 rounded-full text-indigo-200 hover:text-white focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-offset-indigo-600 focus:ring-white",
                span(cls := "sr-only", "View notifications"),
                Icons.outline.bell
              )
            ),
            div(
              cls := "mt-3 px-2 space-y-1",
              a(
                href := "#",
                cls := "block px-3 py-2 rounded-md text-base font-medium text-white hover:bg-indigo-500 hover:bg-opacity-75",
                "Your Profile"
              ),
              a(
                href := "#",
                cls := "block px-3 py-2 rounded-md text-base font-medium text-white hover:bg-indigo-500 hover:bg-opacity-75",
                "Settings"
              ),
              a(
                href := "#",
                cls := "block px-3 py-2 rounded-md text-base font-medium text-white hover:bg-indigo-500 hover:bg-opacity-75",
                "Sign out"
              )
            )
          )
        )
      ),
      header(
        cls := "bg-white shadow-sm",
        div(
          cls := "max-w-7xl mx-auto py-4 px-4 sm:px-6 lg:px-8",
          h1(
            cls := "text-lg leading-6 font-semibold text-gray-900",
            "Dashboard"
          )
        )
      ),
      main(
        div(
          cls := "max-w-7xl mx-auto py-6 sm:px-6 lg:px-8",
          // <!-- Replace with your content -->
          div(
            cls := "px-4 py-4 sm:px-0",
            div(
              cls := "border-4 border-dashed border-gray-200 rounded-lg h-96",
              content
            )
          )
          // <!-- /End replace -->
        )
      )
    )
}
