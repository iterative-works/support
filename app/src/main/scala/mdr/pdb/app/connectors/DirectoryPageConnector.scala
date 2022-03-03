package mdr.pdb.app
package connectors

import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.Router
import mdr.pdb.UserInfo
import mdr.pdb.app.components.PageLink
import mdr.pdb.app.components.AppPage

case class DirectoryPageConnector(
    $input: EventStream[List[UserInfo]],
    actionBus: Observer[Action]
)(using router: Router[Page]):
  val $data = $input.startWithNone
  val $actionSignal = EventStream.fromValue(FetchDirectory)

  def apply: HtmlElement =
    AppPage(actionBus)(
      $data.split(_ => ())((_, _, s) =>
        pages.directory.DirectoryPage(
          s.map(
            _.map(
              _.toUserRow(u =>
                PageLink.container(
                  Page.Detail(Page.Titled(u.personalNumber)),
                  actionBus
                )
              )
            )
          )
        )
      ),
      $actionSignal --> actionBus
    )
