package works.iterative.ui.model

/** Representation of a label or tag.
  *
  * The UI renderer will recognize this value as tag and render accordingly. The
  * value is used to determine both the color of the tag and the text displayed.
  *
  * @param value
  *   the value of the tag
  */
opaque type Tag = String

object Tag:
  def apply(value: String): Tag = value

  extension (tag: Tag) def value: String = tag
