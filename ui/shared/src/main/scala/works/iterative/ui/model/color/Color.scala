package works.iterative.ui.model.color

/** Complete color definition that can be rendered to CSS.
  *
  * Includes the area, kind and weight of the color.
  */
case class Color(area: ColorArea, color: ColorDef):
  def toCSS: String = s"${area.name}-${color.toCSS}"

object Color:
  import ColorDef.given

  def current = ColorDef(ColorKind.current)
  def inherit = ColorDef(ColorKind.inherit)
  def transp = ColorDef(ColorKind.transp)
  def auto = ColorDef(ColorKind.auto)
  def black = ColorDef(ColorKind.black)
  def white = ColorDef(ColorKind.white)
  def slate(weight: ColorWeight) = ColorDef(ColorKind.slate, weight)
  def gray(weight: ColorWeight) = ColorDef(ColorKind.gray, weight)
  def zinc(weight: ColorWeight) = ColorDef(ColorKind.zinc, weight)
  def neutral(weight: ColorWeight) = ColorDef(ColorKind.neutral, weight)
  def stone(weight: ColorWeight) = ColorDef(ColorKind.stone, weight)
  def red(weight: ColorWeight) = ColorDef(ColorKind.red, weight)
  def orange(weight: ColorWeight) = ColorDef(ColorKind.orange, weight)
  def amber(weight: ColorWeight) = ColorDef(ColorKind.amber, weight)
  def yellow(weight: ColorWeight) = ColorDef(ColorKind.yellow, weight)
  def lime(weight: ColorWeight) = ColorDef(ColorKind.lime, weight)
  def green(weight: ColorWeight) = ColorDef(ColorKind.green, weight)
  def emerald(weight: ColorWeight) = ColorDef(ColorKind.emerald, weight)
  def teal(weight: ColorWeight) = ColorDef(ColorKind.teal, weight)
  def cyan(weight: ColorWeight) = ColorDef(ColorKind.cyan, weight)
  def sky(weight: ColorWeight) = ColorDef(ColorKind.sky, weight)
  def blue(weight: ColorWeight) = ColorDef(ColorKind.blue, weight)
  def indigo(weight: ColorWeight) = ColorDef(ColorKind.indigo, weight)
  def violet(weight: ColorWeight) = ColorDef(ColorKind.violet, weight)
  def purple(weight: ColorWeight) = ColorDef(ColorKind.purple, weight)
  def fuchsia(weight: ColorWeight) = ColorDef(ColorKind.fuchsia, weight)
  def pink(weight: ColorWeight) = ColorDef(ColorKind.pink, weight)
  def rose(weight: ColorWeight) = ColorDef(ColorKind.rose, weight)
