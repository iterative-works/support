package works.iterative.core

/** A simple holder for _who_ did _when_, meant to be bound to the _what_ */
final case class EventRecord(userHandle: UserHandle, timestamp: Moment)
