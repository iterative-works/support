package cz.e_bs.cmi.mdr.pdb.app.components

import com.raquo.laminar.api.L.{*, given}
import cz.e_bs.cmi.mdr.pdb.app.Page
import cz.e_bs.cmi.mdr.pdb.{UserProfile, UserInfo, OsobniCislo}
import cz.e_bs.cmi.mdr.pdb.waypoint.components.Navigator
import com.raquo.waypoint.Router

object AppPage:
  // TODO: pages by logged in user
  val pages = List(Page.Directory, Page.Dashboard)

  import NavigationBar.{Logo, MenuItem}

  val logo = Logo(
    "https://tailwindui.com/img/logos/workflow-mark-indigo-300.svg",
    "Workflow"
  )

  // TODO: menu items by user profile
  val userMenu =
    List(
      MenuItem("Your Profile"),
      MenuItem("Settings"),
      MenuItem("Sign out")
    )

  // TODO: load user profile
  val $userProfile = Var(
    UserProfile(
      "tom",
      UserInfo(
        OsobniCislo("1031"),
        "tom",
        "Tom",
        "Cook",
        None,
        None,
        Some("tom@example.com"),
        Some("+420 222 866 180"),
        Some("ČMI Medical"),
        Some("ředitel"),
        None
      )
    )
  )

  val $userInfo = $userProfile.signal.map(_.userInfo)

  type ViewModel = Option[HtmlElement]
  def render($m: Signal[ViewModel], mods: Modifier[HtmlElement]*)(using
      Router[Page]
  ): HtmlElement =
    PageLayout.render(
      $m.combineWith($userInfo).map((c, u) =>
        PageLayout.ViewModel(
          NavigationBar.ViewModel(
            u,
            pages,
            userMenu,
            logo
          ),
          c
        )
      ),
      mods
    )
