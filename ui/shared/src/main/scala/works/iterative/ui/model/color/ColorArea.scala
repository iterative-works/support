package works.iterative.ui.model.color

/** Defines the area the color should apply to, eg. background, text, border,
  * etc.
  */
enum ColorArea(val name: String):
  case bg extends ColorArea("bg")
  case text extends ColorArea("text")
  case decoration extends ColorArea("decoration")
  case border extends ColorArea("border")
  case outline extends ColorArea("outline")
  case divide extends ColorArea("divide")
  case ring extends ColorArea("ring")
  case ringOffset extends ColorArea("ring-offset")
  case shadow extends ColorArea("shadow")
  case accent extends ColorArea("accent")
