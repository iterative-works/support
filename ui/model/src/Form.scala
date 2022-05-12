package works.iterative
package ui.model

case class FormItem[Value](
    id: String,
    label: OneLine,
    description: Option[Paragraph],
    value: Value
)

case class FormSection(
    header: OneLine,
    description: Option[Paragraph],
    items: List[FormItem[_]]
)

case class Form(sections: List[FormSection])
