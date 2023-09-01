package works.iterative.core

import java.time.Instant
import scala.Conversion

/** A moment in time, represented as an instant
  *
  * Why not to use Instant by itself? Well, the time representation might change
  * for some reason, or might be different in different contexts (Like ScalaJS
  * or Native). Better to abstract it away.
  */
opaque type Moment = Instant

object Moment:
  def apply(value: Instant): Moment = value
  extension (m: Moment) def toInstant: Instant = m
  given Conversion[Instant, Moment] = Moment(_)
  given Conversion[Moment, Instant] = _.toInstant
