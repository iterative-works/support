package works.iterative
package ui.model

import core.*

case class FormItem[Value](
    id: String,
    label: PlainOneLine,
    description: Option[PlainMultiLine],
    value: Value
)

case class FormSection(
    header: PlainOneLine,
    description: Option[PlainMultiLine],
    items: List[FormItem[_]]
)

case class Form(sections: List[FormSection])
