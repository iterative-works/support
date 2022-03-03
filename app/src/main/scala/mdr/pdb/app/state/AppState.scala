package mdr.pdb.app
package state

import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.airstream.state.Val
import mdr.pdb.{UserInfo, OsobniCislo}
import com.raquo.airstream.core.Observer
import scala.scalajs.js
import scala.scalajs.js.JSON
import zio.json._
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.ownership.Owner
import com.raquo.waypoint.Router
import mdr.pdb.Parameter
import mdr.pdb.ParameterCriteria
import mdr.pdb.UserFunction
import mdr.pdb.UserContract
import fiftyforms.services.files.File

trait AppState
    extends components.AppPage.AppState
    with connectors.DirectoryPageConnector.AppState
    with connectors.DetailPageConnector.AppState
    with connectors.DetailParametruPageConnector.AppState
    with connectors.DetailKriteriaPageConnector.AppState
    with pages.detail.UpravDukaz.State:

  def online: Signal[Boolean]
  def users: EventStream[List[UserInfo]]
  def details: EventStream[UserInfo]
  def parameters: EventStream[List[Parameter]]
  def actionBus: Observer[Action]

class MockAppState(implicit owner: Owner, router: Router[Page])
    extends AppState:

  given JsonDecoder[OsobniCislo] = JsonDecoder.string.map(OsobniCislo.apply)
  given JsonDecoder[UserFunction] = DeriveJsonDecoder.gen
  given JsonDecoder[UserContract] = DeriveJsonDecoder.gen
  given JsonDecoder[UserInfo] = DeriveJsonDecoder.gen

  given JsonDecoder[ParameterCriteria] = DeriveJsonDecoder.gen
  given JsonDecoder[Parameter] = DeriveJsonDecoder.gen

  private val actions = EventBus[Action]()
  private val (parametersStream, pushParameters) =
    EventStream.withCallback[List[Parameter]]
  private val (usersStream, pushUsers) =
    EventStream.withCallback[List[UserInfo]]
  private val (detailsStream, pushDetails) = EventStream.withCallback[UserInfo]
  private val (filesStream, pushFiles) = EventStream.withCallback[List[File]]

  private val mockData: List[UserInfo] =
    mockUsers
      .asInstanceOf[js.Dictionary[js.Object]]
      .values
      // TODO: is there a more efficient way to parse from JS object directly?
      .map { o =>
        val parsed = JSON.stringify(o).fromJson[UserInfo]
        parsed.left.foreach(org.scalajs.dom.console.log(_))
        parsed
      }
      .collect { case Right(u) =>
        u
      }
      .toList

  private val mockParameters: List[Parameter] =
    pdbParams
      .asInstanceOf[js.Dictionary[js.Object]]
      .values
      .map(o => JSON.stringify(o).fromJson[Parameter])
      .collect { case Right(p) => p }
      .toList

  // TODO: Extract to separate event handler
  actions.events.foreach {
    case FetchDirectory => pushUsers(mockData)
    case FetchUserDetails(osc) =>
      mockData.find(_.personalNumber == osc).foreach { o =>
        pushDetails(o)
        router.replaceState(Page.Detail(o))
      }
    case FetchParameters(osc) =>
      pushParameters(mockParameters)
    case FetchParameter(osc, paramId) =>
      for
        o <- mockData.find(_.personalNumber == osc)
        p <- mockParameters.find(_.id == paramId)
      do
        pushDetails(o)
        pushParameters(mockParameters)
        router.replaceState(Page.DetailParametru(o, p))
    case FetchParameterCriteria(osc, paramId, critId, page) =>
      for
        o <- mockData.find(_.personalNumber == osc)
        p <- mockParameters.find(_.id == paramId)
        c <- p.criteria.find(_.id == critId)
      do
        pushDetails(o)
        pushParameters(mockParameters)
        router.replaceState(page(o, p, c))
    case NavigateTo(page) => router.pushState(page)
    case FetchAvailableFiles(osc) =>
      pushFiles(
        List(
          File("https://tc163.cmi.cz/here", "Example file")
        )
      )
  }

  override def online: Signal[Boolean] = Val(false)
  override def users: EventStream[List[UserInfo]] =
    usersStream.debugWithName("users")

  override def details: EventStream[UserInfo] =
    detailsStream.debugWithName("details")

  override def parameters: EventStream[List[Parameter]] =
    parametersStream.debugWithName("parameters")

  override def availableFiles: EventStream[List[File]] =
    filesStream.debugWithName("available files")

  override def actionBus: Observer[Action] =
    actions.writer.debugWithName("actions writer")
