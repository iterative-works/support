package cz.e_bs.cmi.mdr.pdb.app.components

enum ColorWeight(value: String):
  inline def toCSS: String = value
  // To be used for black and white until
  // we support better mechanism via extension methods
  case w__ extends ColorWeight("")
  case w50 extends ColorWeight("50")
  case w100 extends ColorWeight("100")
  case w200 extends ColorWeight("200")
  case w300 extends ColorWeight("300")
  case w400 extends ColorWeight("400")
  case w500 extends ColorWeight("500")
  case w600 extends ColorWeight("600")
  case w700 extends ColorWeight("700")
  case w800 extends ColorWeight("800")
  case w900 extends ColorWeight("900")

enum Color(name: String):
  import ColorWeight._

  inline def toCSSNoColorWeight(prefix: String): String =
    s"${prefix}-${name}"
  inline def toCSSWithColorWeight(
      prefix: String,
      weight: ColorWeight
  ): String =
    s"${prefix}-${name}-${weight.toCSS}"
  inline def toCSS(prefix: String)(weight: ColorWeight): String =
    weight match {
      case `w__` => toCSSNoColorWeight(prefix)
      case _     => toCSSWithColorWeight(prefix, weight)
    }
  inline def bg: ColorWeight => String = toCSS("bg")(_)
  inline def text: ColorWeight => String = toCSS("text")(_)
  inline def decoration: ColorWeight => String = toCSS("decoration")(_)
  inline def border: ColorWeight => String = toCSS("border")(_)
  inline def outline: ColorWeight => String = toCSS("outline")(_)
  inline def divide: ColorWeight => String = toCSS("divide")(_)
  inline def ring: ColorWeight => String = toCSS("ring")(_)
  inline def ringOffset: ColorWeight => String = toCSS("ring-offset")(_)
  inline def shadow: ColorWeight => String = toCSS("shadow")(_)
  inline def accent: ColorWeight => String = toCSS("accent")(_)

  // TODO: change the "stupid" methods to extension methods
  // that will keep the invariants in comments lower
  case current extends Color("current")
  case inherit extends Color("inherit")
  // Not present in for all methods
  case transp extends Color("transparent")
  // Seen in accent, not preset otherwise
  case auto extends Color("auto")
  // Black and white do not have weight
  case black extends Color("black")
  case white extends Color("white")
  case slate extends Color("slate")
  case gray extends Color("gray")
  case zinc extends Color("zinc")
  case neutral extends Color("neutral")
  case stone extends Color("stone")
  case red extends Color("red")
  case orange extends Color("orange")
  case amber extends Color("amber")
  case yellow extends Color("yellow")
  case lime extends Color("lime")
  case green extends Color("green")
  case emerald extends Color("emerald")
  case teal extends Color("teal")
  case cyan extends Color("cyan")
  case sky extends Color("sky")
  case blue extends Color("blue")
  case indigo extends Color("indigo")
  case violet extends Color("violet")
  case purple extends Color("purple")
  case fuchsia extends Color("fuchsia")
  case pink extends Color("pink")
  case rose extends Color("rose")
