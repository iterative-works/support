package cz.e_bs.cmi.mdr.pdb.app.components

import com.raquo.laminar.api.L.{*, given}
import cz.e_bs.cmi.mdr.pdb.app.Page
import cz.e_bs.cmi.mdr.pdb.app.{UserProfile, UserInfo => ModelUserInfo}
import cz.e_bs.cmi.mdr.pdb.waypoint.components.Navigator

trait AppPage
    extends PageLayout
    with PageHeader
    with Breadcrumbs
    with NavigationBar[Page]
    with Navigator[Page]:
  // TODO: pages by logged in user
  val pages = List(Page.Directory, Page.Dashboard)

  override val logo = Logo(
    "https://tailwindui.com/img/logos/workflow-mark-indigo-300.svg",
    "Workflow"
  )

  // TODO: menu items by user profile
  override val userMenu =
    List(
      MenuItem("Your Profile"),
      MenuItem("Settings"),
      MenuItem("Sign out")
    )

  override def pageTitle(page: Page): String = page.title

  // TODO: load user profile
  val $userProfile = Var(
    UserProfile(
      "tom",
      ModelUserInfo(
        "Tom Cook",
        "tom@example.com",
        "+420 222 866 180",
        None,
        "ČMI Medical",
        "ředitel"
      )
    )
  )

  override val $userInfo = $userProfile.signal.map(p =>
    UserInfo(p.userInfo.name, p.userInfo.email, p.userInfo.img)
  )
