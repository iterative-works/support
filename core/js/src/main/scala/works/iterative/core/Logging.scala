package works.iterative.core

/** Logging app service
  *
  * This typeclass will be implemented by a JS app to provide logging.
  */
trait Logging[A]:
  def log(level: LogLevel, msg: String, t: Option[Throwable]): Unit
  def logError(msg: String, t: Throwable): Unit = logError(msg, Some(t))
  def logError(msg: String, t: Option[Throwable]): Unit =
    log(LogLevel.Error, msg, t)
  def logWarning(msg: String): Unit = log(LogLevel.Warning, msg, None)
  def logInfo(msg: String): Unit = log(LogLevel.Info, msg, None)
  def logDebug(msg: String): Unit = log(LogLevel.Debug, msg, None)
  def logTrace(msg: String): Unit = log(LogLevel.Trace, msg, None)

enum LogLevel:
  case Error, Warning, Info, Debug, Trace
