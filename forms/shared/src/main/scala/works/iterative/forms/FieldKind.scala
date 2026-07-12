// PURPOSE: Closed field-type vocabulary that interpreters dispatch on exhaustively
// PURPOSE: Wire-stable codec: known ids decode to their kind, unknown ids stay Custom byte-exactly

package portaly
package forms

enum NumberKind:
    case Real, Natural, NonNegativeReal

enum FieldKind:
    case Text, Hidden, Date, Prose, Select, Checkbox
    case Number(numbers: NumberKind)
    case Email, Phone, Zip, Country, Ruian
    case Custom(id: String)

    /** The canonical wire id; Custom encodes its original id so unknown vocabulary round-trips
      * byte-exactly. Alias spellings ("text", "email") canonicalize.
      */
    def wireId: String = this match
        case Text                               => "string"
        case Hidden                             => "hidden"
        case Date                               => "date"
        case Prose                              => "prose"
        case Select                             => "select"
        case Checkbox                           => "checkbox"
        case Number(NumberKind.Real)            => "number"
        case Number(NumberKind.Natural)         => "number:natural"
        case Number(NumberKind.NonNegativeReal) => "number:real_non_negative"
        case Email                              => "base:email"
        case Phone                              => "base:phone"
        case Zip                                => "base:zip"
        case Country                            => "base:country"
        case Ruian                              => "base:ruian"
        case Custom(id)                         => id
end FieldKind

object FieldKind:
    /** Decodes a wire field-type id. Unknown ids become Custom so stored declarations always load;
      * per-target handling of Custom lives with each interpreter.
      */
    def of(id: String): FieldKind = id match
        case "string" | "text"          => Text
        case "hidden"                   => Hidden
        case "date"                     => Date
        case "prose"                    => Prose
        case "select"                   => Select
        case "checkbox"                 => Checkbox
        case "number"                   => Number(NumberKind.Real)
        case "number:natural"           => Number(NumberKind.Natural)
        case "number:real_non_negative" => Number(NumberKind.NonNegativeReal)
        case "email" | "base:email"     => Email
        case "base:phone"               => Phone
        case "base:zip"                 => Zip
        case "base:country"             => Country
        case "base:ruian"               => Ruian
        case other                      => Custom(other)
end FieldKind
