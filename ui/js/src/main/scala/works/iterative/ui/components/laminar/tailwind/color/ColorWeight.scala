package works.iterative.ui.components.laminar.tailwind.color

opaque type ColorWeight = String

extension (c: ColorWeight) def value: String = c

/** Defines weight of a color, eg. 50, 100, 200, etc.
  *
  * Tailwind-like.
  */
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
end ColorWeight
