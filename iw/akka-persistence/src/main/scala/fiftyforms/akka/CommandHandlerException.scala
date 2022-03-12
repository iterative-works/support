package works.iterative.akka

// Base class for all command-processing related exceptions from handlers
sealed abstract class CommandHandlerException(
    msg: String,
    cause: Option[Throwable] = None
) extends Exception(msg, cause.orNull)

// TODO: use a typeclass like "Show" to create the error message
case class CommandNotAvailable[C, S](cmd: C, state: S)
    extends CommandHandlerException(
      s"Příkaz $cmd není dostupný ve stavu $state"
    )

// TODO: use a typeclass like "Show" to create the error message
case class CommandRejected[C, S](reason: String, cmd: C, state: S)
    extends CommandHandlerException(
      s"Příkaz $cmd byl ve stavu $state odmítnut s odůvodněním $reason"
    )
