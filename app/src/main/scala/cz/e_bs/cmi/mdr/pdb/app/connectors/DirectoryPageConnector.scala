package cz.e_bs.cmi.mdr.pdb.app
package connectors

import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.Router
import cz.e_bs.cmi.mdr.pdb.UserInfo
import cz.e_bs.cmi.mdr.pdb.app.components.PageLink
import cz.e_bs.cmi.mdr.pdb.app.components.AppPage

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
