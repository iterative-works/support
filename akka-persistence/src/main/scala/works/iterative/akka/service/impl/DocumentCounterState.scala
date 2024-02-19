package works.iterative
package akka
package service.impl

import zio.json.*

case class DocumentCounterState(counter: Int)

object DocumentCounterState:
    given JsonCodec[DocumentCounterState] =
        DeriveJsonCodec.gen[DocumentCounterState]

sealed trait DocumentCounterEvent
object DocumentCounterEvent:
    case object Incremented extends DocumentCounterEvent

given JsonCodec[DocumentCounterEvent] =
    DeriveJsonCodec.gen[DocumentCounterEvent]

class DocumentCounterEventSerializer
    extends AkkaZioJsonSerializer[DocumentCounterEvent](
        identifier = 1241001,
        manifestDiscriminator = _.getClass.getSimpleName
    )

class DocumentCounterStateSerializer
    extends AkkaZioJsonSerializer[DocumentCounterState](
        identifier = 1241002,
        manifestDiscriminator = _.getClass.getSimpleName
    )
