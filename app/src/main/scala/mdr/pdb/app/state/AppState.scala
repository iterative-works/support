package mdr.pdb.app
package state

import zio.*
import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.airstream.state.{Val, Var}
import mdr.pdb.OsobniCislo
import mdr.pdb.users.query.*
import com.raquo.airstream.core.Observer
import scala.scalajs.js
import scala.scalajs.js.JSON
import zio.json.{*, given}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.ownership.Owner
import com.raquo.waypoint.Router
import mdr.pdb.parameters.*
import fiftyforms.services.files.File
import sttp.tapir.DecodeResult
import com.raquo.airstream.ownership.OneTimeOwner
import scala.annotation.unused
import com.raquo.airstream.ownership.Subscription
import mdr.pdb.users.query.client.UsersRepository

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

object AppStateLive:
  def layer: URLayer[
    ZEnv & AppConfig & Api & UsersRepository & Router[Page],
    AppState
  ] = {
    (ZLayer.fromZIO(ZIO.runtime[ZEnv]) ++ ZIOOwner.layer) >>> (
        (
            appConfig: AppConfig,
            api: Api,
            usersRepository: UsersRepository,
            router: Router[Page],
            runtime: Runtime[ZEnv],
            owner: Owner
        ) =>
          AppStateLive(appConfig, api, usersRepository, router, runtime)(using
            owner
          )
    ).toLayer[AppState]
  }

class AppStateLive(
    appConfig: AppConfig,
    api: Api,
    usersRepository: UsersRepository,
    router: Router[Page],
    runtime: Runtime[ZEnv]
)(using
    owner: Owner
) extends AppState:

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
  private val isOnline = Var(true)

  private val mockData: List[UserInfo] =
    mockUsers
      .asInstanceOf[js.Dictionary[js.Object]]
      .values
      // TODO: is there a more efficient way to parse from JS object directly?
      .map(JSON.stringify(_).fromJson[UserInfo])
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

  private def scheduleOnlineCheck(): Unit =
    appConfig.onlineCheckMs.foreach(d =>
      actions.writer.delay(d).onNext(CheckOnlineState)
    )

  // TODO: Extract to separate event handler
  private val handler: Action => Task[Unit] =
    case CheckOnlineState =>
      for
        o <- api.alive()
        _ <- Task.attempt {
          isOnline.set(o)
          scheduleOnlineCheck()
        }
      yield ()
    case FetchDirectory =>
      for
        users <- usersRepository.list()
        _ <- Task.attempt(pushUsers(users))
      yield ()
    case FetchUserDetails(osc) =>
      Task.attempt {
        mockData.find(_.personalNumber == osc).foreach { o =>
          pushDetails(o)
          router.replaceState(Page.Detail(o))
        }
      }
    case FetchParameters(osc) =>
      Task.attempt(pushParameters(mockParameters))
    case FetchParameter(osc, paramId) =>
      Task.attempt {
        for
          o <- mockData.find(_.personalNumber == osc)
          p <- mockParameters.find(_.id == paramId)
        do
          pushDetails(o)
          pushParameters(mockParameters)
          router.replaceState(Page.DetailParametru(o, p))
      }
    case FetchParameterCriteria(osc, paramId, critId, page) =>
      Task.attempt {
        for
          o <- mockData.find(_.personalNumber == osc)
          p <- mockParameters.find(_.id == paramId)
          c <- p.criteria.find(_.id == critId)
        do
          pushDetails(o)
          pushParameters(mockParameters)
          router.replaceState(page(o, p, c))
      }
    case NavigateTo(page) => Task.attempt { router.pushState(page) }
    case FetchAvailableFiles(osc) =>
      Task.attempt {
        pushFiles(
          List(
            File("https://tc163.cmi.cz/here", "Example file")
          )
        )
      }

  actions.events.foreach(action => runtime.unsafeRunAsync(handler(action)))

  scheduleOnlineCheck()

  override def online: Signal[Boolean] = isOnline.signal
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
