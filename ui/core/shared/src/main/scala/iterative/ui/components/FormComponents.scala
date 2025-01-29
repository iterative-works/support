package works.iterative.ui
package components

import works.iterative.forms.FormField
import works.iterative.forms.FormContext

trait FormComponents[T] extends Components[T]:
    import FormComponents.*

    // Context-aware field rendering
    def renderField(
        field: FormField,
        fieldType: FieldType,
        required: Boolean = false,
        default: String = "",
        options: Seq[(String, String)] = Seq.empty,
        attrs: Seq[(String, String)] = Seq.empty
    )(using context: FormContext): T

    // Context-aware field rendering
    def hiddenFields(fieldNames: Seq[FormField])(using FormContext): T

    /** Form container with standard styling */
    def form(action: String)(content: T*): T

    /** Form section with title */
    def formSection(title: String)(content: T*): T

    /** Form row/group for field organization */
    def formRow(content: T): T

    /** Text input field with label and optional help text */
    def textField(
        name: String,
        label: String,
        value: String = "",
        helpText: Option[String] = None,
        required: Boolean = false
    ): T

    /** Date input field */
    def dateField(
        name: String,
        label: String,
        value: String = "",
        helpText: Option[String] = None,
        required: Boolean = false
    ): T

    /** Select input field */
    def selectField(
        name: String,
        label: String,
        options: Seq[(String, String)],
        value: String = "",
        helpText: Option[String] = None,
        required: Boolean = false
    ): T

    /** Hidden field */
    def hiddenField(
        name: String,
        value: String
    ): T

    /** Button section with consistent layout */
    def buttonSection(content: T*): T

    /** Primary action button */
    def primaryButton(
        text: String,
        action: String
    ): T

    /** Secondary action button */
    def secondaryButton(
        text: String,
        action: String
    ): T

    /** Form container with consistent width and spacing */
    def formContainer(content: T): T
end FormComponents

object FormComponents:
    enum FieldType:
        case Text, Date, Select
