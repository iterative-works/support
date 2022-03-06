package mdr.pdb.app.components

import com.raquo.laminar.api.L.{*, given}
import mdr.pdb.app.Page
import mdr.pdb.users.query.{UserProfile, UserInfo}
import mdr.pdb.OsobniCislo
import com.raquo.waypoint.Router
import mdr.pdb.app.Action
import mdr.pdb.app.NavigateTo
import mdr.pdb.users.query.UserFunction

object AppPage:
  trait AppState {
    def online: Signal[Boolean]
    def actionBus: Observer[Action]
  }

  // TODO: pages by logged in user
  val pages: List[Page] = List(Page.Directory, Page.Dashboard)

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
        Some(
          UserFunction("administrátor zakázek", "ředitelství", "ČMI Medical")
        ),
        Nil,
        None
      )
    )
  )

  val $userInfo = $userProfile.signal.map(_.userInfo)

  type ViewModel = Option[HtmlElement]

  def apply(
      state: AppState
  )($m: Signal[ViewModel], mods: Modifier[HtmlElement]*)(using
      router: Router[Page]
  ): HtmlElement =
    PageLayout(state.actionBus)(
      $m.combineWith($userInfo, router.$currentPage, state.online).map(
        (c, u, cp, o) =>
          PageLayout.ViewModel(
            NavigationBar.ViewModel(
              u,
              pages.map(p =>
                NavigationBar.Link(
                  () =>
                    PageLink(
                      p,
                      state.actionBus
                    ),
                  p == cp
                )
              ),
              userMenu,
              logo,
              o
            ),
            c
          )
      ),
      mods
    )
