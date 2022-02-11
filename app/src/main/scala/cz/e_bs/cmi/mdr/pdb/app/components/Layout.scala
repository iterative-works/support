package cz.e_bs.cmi.mdr.pdb.app.components

import com.raquo.domtypes.generic.codecs.StringAsIsCodec
import com.raquo.laminar.api.L.{*, given}
import cz.e_bs.cmi.mdr.pdb.app.{Page, UserProfile}
import com.raquo.waypoint.Router

def PageHeader(using router: Router[Page]): HtmlElement =
  header(
    cls := "bg-white shadow-sm",
    div(
      cls := "max-w-7xl mx-auto py-4 px-4 sm:px-6 lg:px-8",
      h1(
        cls := "text-lg leading-6 font-semibold text-gray-900",
        Breadcrumbs
      )
    )
  )

def MainSection(mods: Modifier[HtmlElement]*): HtmlElement =
  main(mods)

def Layout(
    logo: Navigation.Logo,
    profile: Signal[UserProfile],
    pages: Signal[List[Page]],
    userMenu: Signal[List[Navigation.MenuItem]],
    content: HtmlElement
)(using router: Router[Page]): HtmlElement =
  div(
    cls := "min-h-full",
    Navigation(
      logo,
      profile,
      pages,
      userMenu
    ),
    PageHeader,
    content
  )
