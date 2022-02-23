package cz.e_bs.cmi.mdr.pdb.app.components

import com.raquo.laminar.api.L.{*, given}
import cz.e_bs.cmi.mdr.pdb.waypoint.components.Navigator
import CustomAttrs.ariaCurrent
import com.raquo.waypoint.Router
import cz.e_bs.cmi.mdr.pdb.UserInfo
import cz.e_bs.cmi.mdr.pdb.app.Page

object NavigationBar:

  case class Logo(img: String, name: String)
  case class MenuItem(title: String)

  case class ViewModel(
      userInfo: UserInfo,
      pages: List[Page],
      userMenu: List[MenuItem],
      logo: Logo
  )

  def render($m: Signal[ViewModel])(using router: Router[Page]): HtmlElement =
    val $userInfo = $m.map(_.userInfo)
    val mobileMenuOpen = Var(false)

    val desktopOnly = cls("hidden md:block")
    val mobileOnly = cls("md:hidden")

    inline def avatarImage(size: Int = 8) =
      Avatar($userInfo.map(_.img)).avatarImage(size)

    def notificationButton = button(
      tpe := "button",
      cls := List(
        "bg-indigo-600",
        "focus:outline-none",
        "focus:ring-2",
        "focus:ring-offset-2",
        "focus:ring-offset-indigo-600",
        "focus:ring-white",
        "hover:text-white",
        "p-1",
        "rounded-full",
        "text-indigo-200"
      ),
      span(cls := "sr-only", "View notifications"),
      Icons.outline.bell
    )

    def userProfile: HtmlElement =
      val menuOpen = Var(false)

      def menuItem(item: MenuItem, idx: Int): HtmlElement =
        a(
          href := "#",
          cls := "block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100",
          role := "menuitem",
          tabIndex := -1,
          idAttr := s"user-menu-item-$idx",
          item.title
        )

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
            child <-- avatarImage(),
            onClick.preventDefault.mapTo(
              !menuOpen.now()
            ) --> menuOpen.writer
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
          // : keyboard navigation
          children <-- $m.map(_.userMenu.zipWithIndex.map(menuItem))
        )
      )

    def mobileProfile =
      def menuItem(item: MenuItem): HtmlElement =
        a(
          href := "#",
          cls := "block px-3 py-2 rounded-md text-base font-medium text-white hover:bg-indigo-500 hover:bg-opacity-75",
          item.title
        )

      div(
        cls := "pt-4 pb-3 border-t border-indigo-700",
        div(
          cls := "flex items-center px-5",
          div(
            cls := "flex-shrink-0",
            child <-- avatarImage(10)
          ),
          div(
            cls := "ml-3",
            div(
              cls := "text-base font-medium text-white",
              child.text <-- $userInfo.map(_.name)
            ),
            child.maybe <-- $userInfo.map(
              _.email.map(e =>
                div(
                  cls := "text-sm font-medium text-indigo-300",
                  e
                )
              )
            )
          ),
          notificationButton.amend(cls := List("flex-shrink-0", "ml-auto"))
        ),
        div(
          cls := "mt-3 px-2 space-y-1",
          children <-- $m.map(_.userMenu.map(menuItem))
        )
      )

    def pageLink(page: Page, active: Signal[Boolean]): Anchor =
      a(
        Navigator.navigateTo(page),
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

    def logoImg: Image =
      img(
        cls := "h-8 w-8",
        src <-- $m.map(_.logo.img),
        alt <-- $m.map(_.logo.name)
      )

    def pageLinks(mods: Modifier[HtmlElement]*) =
      $m.map(
        _.pages.map(p =>
          pageLink(p, router.$currentPage.map(p == _)).amend(mods)
        )
      )

    def mobileMenuButton = button(
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
      onClick.preventDefault.mapTo(
        !mobileMenuOpen.now()
      ) --> mobileMenuOpen.writer
    )

    def navBarLeft =
      div(
        cls := "flex items-center",
        div(cls := "flex-shrink-0", logoImg),
        div(
          desktopOnly,
          div(
            cls := "ml-10 flex items-baseline space-x-4",
            children <-- pageLinks()
          )
        )
      )

    def navBarRight =
      div(
        desktopOnly,
        div(
          cls := "ml-4 flex items-center md:ml-6",
          notificationButton,
          userProfile
        )
      )

    def navBarMobile =
      div(
        cls := "-mr-2 flex",
        mobileOnly,
        mobileMenuButton
      )

    def navBar =
      div(
        cls := "max-w-7xl mx-auto px-4 sm:px-6 lg:px-8",
        div(
          cls := "flex items-center justify-between h-16",
          navBarLeft,
          navBarRight,
          navBarMobile
        )
      )

    def mobileMenu =
      div(
        mobileOnly,
        cls <-- mobileMenuOpen.signal.map { o =>
          if (o) "block" else "hidden"
        },
        idAttr := "mobile-menu",
        div(
          cls := "px-2 pt-2 pb-3 space-y-1 sm:px-3",
          children <-- pageLinks(cls := "block")
        ),
        mobileProfile
      )

    nav(cls := "bg-indigo-600", navBar, mobileMenu)
