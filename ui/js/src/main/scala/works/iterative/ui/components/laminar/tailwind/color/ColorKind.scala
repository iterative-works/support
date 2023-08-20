package works.iterative.ui.components.laminar.tailwind.color

/** Defines what color should be used, without specifying the area or weight.
  */
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
