package works.iterative.event

trait Event[A]:
  def record: EventRecord
