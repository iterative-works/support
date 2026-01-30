package works.iterative.tapir

import zio.*
import zio.stream.*

/** Run a read-only source from server, publish whatever comes to a hub. Reconnect on failure.
  *
  * @param updateHub
  *   hub to publish updates to
  * @param retrySchedule
  *   schedule for retries, defaults to 2 seconds
  * @tparam A
  *   type of updates
  */
class ReconectingUpdateSource[A](
    updateHub: Hub[A],
    retrySchedule: Schedule[Any, Any, ?] = Schedule.spaced(2.second)
):

    /** Run the update source.
      *
      * It will start the process, reconnecting on failure. Meant to be forked somewhere scoped,
      * `updateSource.run.forkScoped`
      */
    def run[B](
        source: Unit => UIO[
            ZStream[Any, Throwable, B] => ZStream[Any, Throwable, A]
        ]
    ): UIO[Unit] =
        source(())
            .flatMap(
                _(ZStream.never)
                    .foreach(updateHub.publish)
                    .retry(retrySchedule)
                    .orDie
            )
end ReconectingUpdateSource
