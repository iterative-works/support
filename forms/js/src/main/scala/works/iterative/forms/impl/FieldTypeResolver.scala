package works.iterative.forms
package impl

trait FieldTypeResolver:
    def resolve(fieldType: FieldType): FieldFactory[String]
    def withAutocompleteContext(context: Map[String, String]): FieldTypeResolver

object FieldTypeResolver:
    /** The base dispatch without any client factories: plain inputs typed by kind, mirroring the
      * SSR renderer's control table so the two defaults cannot drift.
      */
    val empty: FieldTypeResolver = new FieldTypeResolver:
        override def resolve(fieldType: FieldType): FieldFactory[String] = fieldType.kind match
            case FieldKind.Prose =>
                FieldFactory.TextArea("text", _ => ValidationRule.valid)
            case FieldKind.Hidden => FieldFactory.Hidden()
            case kind =>
                FieldFactory.Text(
                    inputType(kind),
                    !fieldType.disabled,
                    _ => ValidationRule.valid,
                    None
                )
        override def withAutocompleteContext(context: Map[String, String]): FieldTypeResolver =
            this
    end empty

    private def inputType(kind: FieldKind): String = kind match
        case FieldKind.Date  => "date"
        case FieldKind.Email => "email"
        case FieldKind.Phone => "tel"
        // A checkbox input cannot carry the value controller the text factory binds; bool
        // enums are the checkbox story, a checkbox-typed Field stays a text input here
        case FieldKind.Checkbox                          => "text"
        case FieldKind.Number(_)                         => "number"
        case FieldKind.Custom(id @ ("tel" | "password")) => id
        case _                                           => "text"
end FieldTypeResolver
