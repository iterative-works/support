package cz.e_bs.cmi.mdr.pdb.app

import com.raquo.laminar.api.L.{*, given}
import com.raquo.domtypes.generic.codecs.StringAsIsCodec

// Made a pull request to add aria-current to scala-dom-types, remove after
val ariaCurrent = customHtmlAttr("aria-current", StringAsIsCodec)

object Layout:

  def pageLink(page: Page, active: Signal[Boolean]): Anchor =
    a(
      href := "#",
      cls <-- active.map {
        case true  => "bg-indigo-700"
        case false => "hover:bg-indigo-500 hover:bg-opacity-75"
      },
      cls := "text-white px-3 py-2 rounded-md text-sm font-medium",
      ariaCurrent <-- active.map {
        case true => "page"
        case _    => "false"
      },
      page.title
    )

  def logo: HtmlElement =
    img(
      cls := "h-8 w-8",
      src := "https://tailwindui.com/img/logos/workflow-mark-indigo-300.svg",
      alt := "Workflow"
    )

  def navigation(
      pages: Signal[List[Page]],
      activePage: Signal[Page]
  ): HtmlElement = {
    def pageLinks(mods: Modifier[HtmlElement]*) = pages.map(
      _.map(p => pageLink(p, activePage.map(p == _)).amend(mods))
    )

    def notifications = button(
      tpe := "button",
      cls := "p-1 bg-indigo-600 rounded-full text-indigo-200 hover:text-white focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-offset-indigo-600 focus:ring-white",
      span(cls := "sr-only", "View notifications"),
      Icons.outline.bell
    )

    val menuOpen = Var(false)

    val mobileMenuOpen = Var(false)

    def mobileMenuButton() = button(
      tpe := "button",
      cls := "bg-indigo-600 inline-flex items-center justify-center p-2 rounded-md text-indigo-200 hover:text-white hover:bg-indigo-500 hover:bg-opacity-75 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-offset-indigo-600 focus:ring-white",
      aria.controls := "mobile-menu",
      aria.expanded <-- mobileMenuOpen.signal,
      span(cls := "sr-only", "Open main menu"),
      Icons.outline.menu.amend(svg.cls <-- mobileMenuOpen.signal.map { o =>
        if (o) "hidden" else "block"
      }),
      Icons.outline.x.amend(svg.cls <-- mobileMenuOpen.signal.map { o =>
        if (o) "block" else "hidden"
      }),
      onClick.mapTo(!mobileMenuOpen.now()) --> mobileMenuOpen.writer
    )

    nav(
      cls := "bg-indigo-600",
      div(
        cls := "max-w-7xl mx-auto px-4 sm:px-6 lg:px-8",
        div(
          cls := "flex items-center justify-between h-16",
          div(
            cls := "flex items-center",
            div(cls := "flex-shrink-0", logo),
            div(
              cls := "hidden md:block",
              div(
                cls := "ml-10 flex items-baseline space-x-4",
                children <-- pageLinks()
              )
            )
          ),
          div(
            cls := "hidden md:block",
            div(
              cls := "ml-4 flex items-center md:ml-6",
              notifications,

              // <!-- Profile dropdown -->
              div(
                cls := "ml-3 relative",
                div(
                  button(
                    tpe := "button",
                    cls := "max-w-xs bg-indigo-600 rounded-full flex items-center text-sm focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-offset-indigo-600 focus:ring-white",
                    idAttr := "user-menu-button",
                    aria.expanded <-- menuOpen.signal,
                    aria.hasPopup := true,
                    span(cls := "sr-only", "Open user menu"),
                    img(
                      cls := "h-8 w-8 rounded-full",
                      src := "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=facearea&facepad=2&w=256&h=256&q=80",
                      alt := ""
                    ),
                    onClick.mapTo(!menuOpen.now()) --> menuOpen.writer
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
                  cls <-- menuOpen.signal.map { o =>
                    if (o) "md:block" else "md:hidden"
                  },
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
            mobileMenuButton()
          )
        )
      ),

      // <!-- Mobile menu, show/hide based on menu state. -->
      div(
        cls := "md:hidden",
        cls <-- mobileMenuOpen.signal.map { o =>
          if (o) "block" else "hidden"
        },
        idAttr := "mobile-menu",
        div(
          cls := "px-2 pt-2 pb-3 space-y-1 sm:px-3",
          children <-- pageLinks(cls := "block")
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
    )
  }

  def pageHeader(currentPage: Signal[Page]): HtmlElement =
    header(
      cls := "bg-white shadow-sm",
      div(
        cls := "max-w-7xl mx-auto py-4 px-4 sm:px-6 lg:px-8",
        h1(
          cls := "text-lg leading-6 font-semibold text-gray-900",
          child.text <-- currentPage.map(_.title)
        )
      )
    )

  def mainSection(content: HtmlElement): HtmlElement =
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

  def apply(
      pages: Signal[List[Page]],
      currentPage: Signal[Page],
      content: HtmlElement
  ): HtmlElement =
    div(
      cls := "min-h-full",
      navigation(
        pages,
        currentPage
      ),
      pageHeader(currentPage),
      mainSection(content)
    )
