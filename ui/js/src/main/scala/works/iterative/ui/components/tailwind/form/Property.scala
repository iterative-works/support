package works.iterative
package ui.components.tailwind.form

// Property is a named value.
trait Property[V]:
  def id: String
  // Property identification
  def name: String
  // Value
  def value: Option[V]

trait PropertyDescription:
  // Human label
  def label: String
  // Larger description
  def description: Option[String]

trait DescribedProperty[V] extends Property[V] with PropertyDescription

case class FormProperty[V](
    id: String,
    name: String,
    label: String,
    description: Option[String],
    value: Option[V]
) extends Property[V]
    with PropertyDescription
