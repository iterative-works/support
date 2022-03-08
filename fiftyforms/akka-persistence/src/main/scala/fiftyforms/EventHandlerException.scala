package fiftyforms.akka

sealed abstract class EventHandlerException(
    msg: String,
    cause: Option[Throwable] = None
) extends Exception(msg, cause.orNull)

case class UnhandledEvent[Event, State](event: Event, state: State)
    extends EventHandlerException(
      s"Událost $event nastala ve stavu $state bez možnosti zpracování"
    )
