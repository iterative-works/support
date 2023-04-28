package works.iterative.ui.model

import works.iterative.ui.model.color.ColorKind

/** Representation of colored string value.
  *
  * Used generally to represent tags or "labels", eg. some kind of status or
  * categorization.
  */
final case class Tag(value: String, color: ColorKind = ColorKind.gray)
