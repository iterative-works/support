package works.iterative.ui.components.laminar.forms

import works.iterative.core.PlainMultiLine
import works.iterative.ui.components.ComponentContext

trait FieldDescriptor:
    def id: FieldId
    def idString: String
    def name: String
    def label: String
    def help: Option[PlainMultiLine]
    def placeholder: Option[String]
end FieldDescriptor

object FieldDescriptor:
    def apply(fieldId: FieldId)(using ctx: ComponentContext[?]): FieldDescriptor =
        new FieldDescriptor:
            override def id: FieldId = fieldId
            override def idString: String = fieldId
            override def name: String = fieldId
            override def label: String = ctx.messages(fieldId)
            override def help: Option[PlainMultiLine] =
                ctx.messages.get(fieldId + ".help")
            override def placeholder: Option[String] =
                ctx.messages.get(fieldId + ".placeholder")
end FieldDescriptor
