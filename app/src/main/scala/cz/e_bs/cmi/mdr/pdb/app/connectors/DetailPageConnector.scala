package cz.e_bs.cmi.mdr.pdb.app
package connectors

import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.Router
import cz.e_bs.cmi.mdr.pdb.UserInfo
import pages.detail.DetailPage
import cz.e_bs.cmi.mdr.pdb.app.components.AppPage
import cz.e_bs.cmi.mdr.pdb.app.pages.detail.components.DetailOsoby

case class DetailPageConnector(
    $input: EventStream[UserInfo],
    actionBus: Observer[Action],
    $page: Signal[Page.Detail]
)(using router: Router[Page]):
  val $oscChangeSignal = $page.splitOne(_.osobniCislo)((osc, _, _) => osc)
  // TODO: filter the value based on the current osc
  // OSC change will fetch new data, but still
  // - we need to be sure that what we got is really what we ought to display
  // - we want to display stale data accordingly (at least with loading indicator)
  val $data = $input.startWithNone
  val $pageChangeSignal =
    $oscChangeSignal.map(FetchUserDetails.apply)

  def render: HtmlElement =
    AppPage().render(
      $data.map(
        _.map(o =>
          DetailPage.ViewModel(
            DetailOsoby.ViewModel(
              o.personalNumber,
              o.name,
              o.email,
              o.phone,
              o.img,
              None,
              None
            ),
            Nil
          )
        )
      ).split(_ => ())((_, _, s) => DetailPage.render(s)),
      $pageChangeSignal --> actionBus
    )
