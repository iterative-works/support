package cz.e_bs.cmi.mdr.pdb.app

import com.raquo.airstream.core.EventStream
import cz.e_bs.cmi.mdr.pdb.{UserInfo, OsobniCislo}
import com.raquo.airstream.core.Observer
import scala.scalajs.js
import scala.scalajs.js.JSON
import zio.json._
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.ownership.Owner
import com.raquo.waypoint.Router

trait AppState:
  def users: EventStream[List[UserInfo]]
  def details: EventStream[UserInfo]
  def actionBus: Observer[Action]

class MockAppState(implicit owner: Owner, router: Router[Page])
    extends AppState:

  given JsonDecoder[OsobniCislo] = JsonDecoder.string.map(OsobniCislo.apply)
  given JsonDecoder[UserInfo] = DeriveJsonDecoder.gen

  private val actions = EventBus[Action]()
  private val (usersStream, pushUsers) =
    EventStream.withCallback[List[UserInfo]]
  private val (detailsStream, pushDetails) = EventStream.withCallback[UserInfo]

  private val mockData =
    mockUsers
      .asInstanceOf[js.Dictionary[js.Object]]
      .values
      // TODO: is there a more efficient way to parse from JS object directly?
      .map(o => JSON.stringify(o).fromJson[UserInfo])
      .collect { case Right(u) =>
        u
      }
      .toList

  actions.events.foreach {
    case FetchDirectory => pushUsers(mockData)
    case FetchUserDetails(osc) =>
      mockData.find(_.personalNumber == osc).foreach { o =>
        pushDetails(o)
        router.replaceState(Page.Detail(o))
      }
    case NavigateTo(page) => router.pushState(page)
  }

  override def users = usersStream.debugWithName("users")

  override def details = detailsStream.debugWithName("details")

  override def actionBus: Observer[Action] =
    actions.writer.debugWithName("actions writer")
