package works.iterative.app

import zio.*
import com.raquo.laminar.api.L.*
import works.iterative.ui.services.UserNotificationService
import works.iterative.ui.services.UserNotificationService.Level
import works.iterative.core.UserMessage
import works.iterative.core.MessageCatalogue
import works.iterative.ui.laminar.AlertViews

class DOMUserNotificationService(
    messages: MessageCatalogue,
    views: AlertViews
) extends UserNotificationService:

    val displayedNotification: Var[Option[(Level, UserMessage)]] = Var(None)

    val element: HtmlElement = div(
        aria.live("assertive"),
        cls(
            "z-50 pointer-events-none fixed inset-0 flex items-end px-4 py-6 sm:items-start sm:p-6"
        ),
        div(
            cls("flex w-full flex-col items-center space-y-4 sm:items-end"),
            child.maybe <-- displayedNotification.signal.map(
                _.flatMap((l, m) =>
                    def msg = messages
                        .opt(
                            m,
                            (l match
                            case Level.Success => List(UserMessage("success"))
                            case Level.Info    => Nil
                            case Level.Error   => List(UserMessage("error"))
                            case Level.Warning => Nil
                            case Level.Debug   => Nil
                            )*
                        )
                        .map(txt => span(dataAttr("msgId")(m.id.toString), txt))

                    val alertLevel: AlertViews.Level = l match
                    case Level.Success => AlertViews.Level.Success
                    case Level.Info    => AlertViews.Level.Info
                    case Level.Error   => AlertViews.Level.Error
                    case Level.Warning => AlertViews.Level.Warning
                    case Level.Debug   => AlertViews.Level.Debug

                    msg.map(views.alert(alertLevel, _, () => displayedNotification.set(None)))
                ).map(m =>
                    div(
                        cls(
                            "pointer-events-auto w-full max-w-sm overflow-hidden rounded-lg shadow-lg ring-1 ring-black ring-opacity-5"
                        ),
                        m
                    )
                )
            )
        )
    )

    override def notify(level: Level, msg: UserMessage): UIO[Unit] =
        val display = ZIO.succeed(displayedNotification.set(Some(level -> msg)))
        val hide = ZIO.succeed(displayedNotification.set(None))
        level match
        case Level.Debug =>
            ZIO.succeed(messages.get(msg).foreach(org.scalajs.dom.console.debug(_)))
        case Level.Success =>
            display *> hide.delay(5.seconds)
        case _ => display
        end match
    end notify

    def hook: AppExtension = new AppExtension:
        override def appViewHook(appView: HtmlElement): UIO[HtmlElement] =
            ZIO.succeed(appView.amend(element))
end DOMUserNotificationService

object DOMUserNotificationService:
    def layer(views: AlertViews): URLayer[
        AppExtensionService & MessageCatalogue,
        UserNotificationService
    ] = ZLayer {
        for
            messageCatalogue <- ZIO.service[MessageCatalogue]
            service = DOMUserNotificationService(messageCatalogue, views)
            _ <- ZIO.serviceWithZIO[AppExtensionService](
                _.registerExtension(service.hook)
            )
        yield service
    }
end DOMUserNotificationService
