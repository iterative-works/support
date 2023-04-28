package works.iterative.ui.model

/** Marker trait for capabilities */
trait Capability

/** A trait for checking if a capability is supported.
  *
  * The idea is that the components will announce the possible capabilities used
  * in the UI. The model will fill the capabilities with the actual capabilities
  * of the user and the UI will check for a capability before rendering.
  */
trait CapabilityCheck:
  def hasCapability[T <: Capability](c: T): Boolean

/** A set of supported capabilities.
  */
case class Capabilities(capabilities: Set[Capability]) extends CapabilityCheck:
  def hasCapability[T <: Capability](c: T): Boolean = capabilities.contains(c)
