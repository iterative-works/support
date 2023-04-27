package works.iterative.ui.components.tailwind.experimental

import scala.util.NotGiven
import com.raquo.domtypes.generic.Modifier
import org.scalajs.dom.SVGElement

opaque type ColorWeight = String

extension (c: ColorWeight) def value: String = c

object ColorWeight:
  inline given int50: Conversion[50, ColorWeight] with
    inline def apply(i: 50) = "50"
  inline given int100: Conversion[100, ColorWeight] with
    inline def apply(i: 100) = "100"
  inline given int200: Conversion[200, ColorWeight] with
    inline def apply(i: 200) = "200"
  inline given int300: Conversion[300, ColorWeight] with
    inline def apply(i: 300) = "300"
  inline given int400: Conversion[400, ColorWeight] with
    inline def apply(i: 400) = "400"
  inline given int500: Conversion[500, ColorWeight] with
    inline def apply(i: 500) = "500"
  inline given int600: Conversion[600, ColorWeight] with
    inline def apply(i: 600) = "600"
  inline given int700: Conversion[700, ColorWeight] with
    inline def apply(i: 700) = "700"
  inline given int800: Conversion[800, ColorWeight] with
    inline def apply(i: 800) = "800"
  inline given int900: Conversion[900, ColorWeight] with
    inline def apply(i: 900) = "900"
  inline given int950: Conversion[950, ColorWeight] with
    inline def apply(i: 950) = "950"

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

sealed abstract class ColorKind private (val name: String):
  def apply(weight: ColorWeight): ColorDef =
    ColorDef.WeightedColorDef(this, weight)

object ColorKind:
  trait Unweighted:
    self: ColorKind =>
    override def apply(weight: ColorWeight): ColorDef =
      ColorDef.UnweightedColorDef(self)

  // TODO: change the "stupid" methods to extension methods
  // that will keep the invariants in comments lower
  case object current extends ColorKind("current") with Unweighted
  case object inherit extends ColorKind("inherit") with Unweighted
  // Not present in for all methods
  case object transp extends ColorKind("transparent") with Unweighted
  // Seen in accent, not preset otherwise
  case object auto extends ColorKind("auto") with Unweighted
  // Black and white do not have weight
  case object black extends ColorKind("black") with Unweighted
  case object white extends ColorKind("white") with Unweighted
  case object slate extends ColorKind("slate")
  case object gray extends ColorKind("gray")
  case object zinc extends ColorKind("zinc")
  case object neutral extends ColorKind("neutral")
  case object stone extends ColorKind("stone")
  case object red extends ColorKind("red")
  case object orange extends ColorKind("orange")
  case object amber extends ColorKind("amber")
  case object yellow extends ColorKind("yellow")
  case object lime extends ColorKind("lime")
  case object green extends ColorKind("green")
  case object emerald extends ColorKind("emerald")
  case object teal extends ColorKind("teal")
  case object cyan extends ColorKind("cyan")
  case object sky extends ColorKind("sky")
  case object blue extends ColorKind("blue")
  case object indigo extends ColorKind("indigo")
  case object violet extends ColorKind("violet")
  case object purple extends ColorKind("purple")
  case object fuchsia extends ColorKind("fuchsia")
  case object pink extends ColorKind("pink")
  case object rose extends ColorKind("rose")

sealed trait ColorDef:
  def toCSS: String

  def bg = Color(ColorArea.bg, this)
  def text = Color(ColorArea.text, this)
  def decoration = Color(ColorArea.decoration, this)
  def border = Color(ColorArea.border, this)
  def outline = Color(ColorArea.outline, this)
  def divide = Color(ColorArea.divide, this)
  def ring = Color(ColorArea.ring, this)
  def ringOffset = Color(ColorArea.ringOffset, this)
  def shadow = Color(ColorArea.shadow, this)
  def accent = Color(ColorArea.accent, this)

// TODO: create a macro that will output the tailwind class as string directly during compilation, where possible.
object ColorDef:
  case class WeightedColorDef(
      kind: ColorKind,
      weight: ColorWeight
  ) extends ColorDef:
    override def toCSS: String = s"${kind.name}-${weight.value}"

  case class UnweightedColorDef(
      kind: ColorKind
  ) extends ColorDef:
    override def toCSS: String = kind.name

  // TODO: check that the kind is valid unweighted kind
  // that means current, inherit, auto, transparent, black, white
  // tried using implicit evidence, but the type inference for enumerations
  // tends to generalize to the enum, instead of the real type
  def apply[T <: ColorKind](kind: T)(using
      ev: T <:< ColorKind.Unweighted
  ): ColorDef =
    UnweightedColorDef(kind)
  def apply(kind: ColorKind, weight: ColorWeight): ColorDef =
    WeightedColorDef(kind, weight)

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
