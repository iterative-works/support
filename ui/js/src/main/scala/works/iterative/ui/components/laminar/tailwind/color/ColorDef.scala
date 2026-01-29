package works.iterative.ui.components.laminar.tailwind.color

/** A combination of ColorKind and ColorWeight, if applicable.
  *
  * By applying area we get the full Color definition.
  */
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
end ColorDef

// TODO: create a macro that will output the tailwind class as string directly during compilation, where possible.
object ColorDef:
    case class WeightedColorDef(
        kind: ColorKind,
        weight: ColorWeight
    ) extends ColorDef:
        override def toCSS: String = s"${kind.name}-${weight.value}"
    end WeightedColorDef

    case class UnweightedColorDef(
        kind: ColorKind
    ) extends ColorDef:
        override def toCSS: String = kind.name
    end UnweightedColorDef

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
end ColorDef
