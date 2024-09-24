package portaly.forms
package impl

trait FieldTypeResolver:
    def resolve(fieldType: FieldType): FieldFactory[String]
    def withAutocompleteContext(context: Map[String, String]): FieldTypeResolver

object FieldTypeResolver:
    val empty: FieldTypeResolver = new FieldTypeResolver:
        override def resolve(fieldType: FieldType): FieldFactory[String] = fieldType match
            case FieldType("prose", _, _) =>
                FieldFactory.TextArea("text", _ => ValidationRule.valid)
            case FieldType("hidden", _, _) => FieldFactory.Hidden()
            case FieldType(_, _, disabled) =>
                FieldFactory.Text("text", !disabled, _ => ValidationRule.valid, None)
        override def withAutocompleteContext(context: Map[String, String]): FieldTypeResolver =
            this
    end empty
end FieldTypeResolver
