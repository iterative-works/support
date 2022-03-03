package mdr.pdb.app
package connectors

import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.Router
import mdr.pdb.UserInfo
import mdr.pdb.app.components.PageLink
import mdr.pdb.app.components.AppPage

object DirectoryPageConnector:
  trait AppState extends AppPage.AppState:
    def users: EventStream[List[UserInfo]]

class DirectoryPageConnector(state: DirectoryPageConnector.AppState)(using
    router: Router[Page]
):
  def apply: HtmlElement =
    val $data = state.users.startWithNone
    val $actionSignal = EventStream.fromValue(FetchDirectory)

    AppPage(state)(
      $data.split(_ => ())((_, _, s) =>
        pages.directory.DirectoryPage(
          s.map(
            _.map(
              _.toUserRow(u =>
                PageLink.container(
                  Page.Detail(Page.Titled(u.personalNumber)),
                  state.actionBus
                )
              )
            )
          )
        )
      ),
      $actionSignal --> state.actionBus
    )
