package works.iterative.ui.components.laminar.forms

import works.iterative.core.{MessageCatalogue, PlainMultiLine}

trait FieldDescriptor:
    def id: FieldId
    def idString: String
    def name: String
    def label: String
    def help: Option[PlainMultiLine]
    def placeholder: Option[String]
end FieldDescriptor

object FieldDescriptor:
    def apply(fieldId: FieldId)(using messages: MessageCatalogue): FieldDescriptor =
        new FieldDescriptor:
            override def id: FieldId = fieldId
            override def idString: String = fieldId
            override def name: String = fieldId
            override def label: String = messages(fieldId)
            override def help: Option[PlainMultiLine] = messages.get(fieldId + ".help")
            override def placeholder: Option[String] = messages.get(fieldId + ".placeholder")
end FieldDescriptor
