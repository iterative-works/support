package works.iterative.event

import works.iterative.core.*
import zio.*

/** A simple holder for _who_ did _when_, meant to be bound to the _what_ */
final case class EventRecord(userHandle: UserHandle, timestamp: Moment):
    val displayName: String = userHandle.displayName

object EventRecord:
    def apply(userHandle: UserHandle): UIO[EventRecord] =
        Moment.now.map(EventRecord(userHandle, _))

    def now(using userHandle: UserHandle): UIO[EventRecord] =
        apply(userHandle)
end EventRecord
