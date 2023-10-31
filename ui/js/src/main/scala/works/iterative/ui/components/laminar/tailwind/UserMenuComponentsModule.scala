package works.iterative.ui.components.laminar.tailwind.ui

import com.raquo.laminar.api.L.*

trait UserMenuComponentsModule:
  object userMenu:
    def navBarItem(button: HtmlElement, popup: HtmlElement): HtmlElement =
      div(
        cls("relative ml-3"),
        div(button),
        popup
      )

    def userName(name: String): HtmlElement =
      span(cls("text-white"), name)

    def avatar(href: String): HtmlElement =
      img(
        src(href),
        cls("h-8 w-8 rounded-full")
      )

    def menuButton(userDetails: Node*): HtmlElement =
      button(
        tpe("button"),
        cls(
          "flex max-w-xs items-center rounded-full bg-indigo-600 text-sm focus:outline-none focus:ring-2 focus:ring-white focus:ring-offset-2 focus:ring-offset-indigo-600"
        ),
        idAttr("user-menu-button"),
        aria.hasPopup(true),
        userDetails,
        span(cls("sr-only"), "Open user menu")
      )

    def popup(menuItems: HtmlElement*): HtmlElement =
      div(
        cls(
          "absolute right-0 z-10 mt-2 w-48 origin-top-right rounded-md bg-white py-1 shadow-lg ring-1 ring-black ring-opacity-5 focus:outline-none"
        ),
        role("menu"),
        aria.orientation("vertical"),
        aria.labelledBy("user-menu-button"),
        tabIndex(-1),
        menuItems
      )

    def menuItem(id: String, label: Node): HtmlElement =
      a(
        cls("block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"),
        href("#"),
        role("menuitem"),
        tabIndex(-1),
        idAttr(s"user-menu-item-${id}"),
        label
      )
