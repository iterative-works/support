package works.iterative.forms
package impl

trait FieldTypeResolver:
    def resolve(fieldType: FieldType): FieldFactory[String]
    def withAutocompleteContext(context: Map[String, String]): FieldTypeResolver

object FieldTypeResolver:
    val empty: FieldTypeResolver = new FieldTypeResolver:
        override def resolve(fieldType: FieldType): FieldFactory[String] = fieldType.kind match
            case FieldKind.Prose =>
                FieldFactory.TextArea("text", _ => ValidationRule.valid)
            case FieldKind.Hidden => FieldFactory.Hidden()
            case _ =>
                FieldFactory.Text("text", !fieldType.disabled, _ => ValidationRule.valid, None)
        override def withAutocompleteContext(context: Map[String, String]): FieldTypeResolver =
            this
    end empty
end FieldTypeResolver
