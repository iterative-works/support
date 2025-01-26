package works.iterative.ui.model.forms

import works.iterative.core.UserMessage
import works.iterative.core.MessageId

// TODO: use IdPath
type UIFormId = String
type UIMessageKey = MessageId
type UIFieldName = String
type UIFieldType = String
type UIButtonIcon = String

enum UIFieldDecoration:
    case Required
    case InError
    case Disabled
    case ErrorMessage(message: UserMessage)
    case IconButton(id: UIFormId, name: String)
end UIFieldDecoration

final case class UIForm(
    id: UIFormId,
    messageKey: UIMessageKey,
    children: Seq[UIFormElement],
    data: FormState,
    context: Option[Map[String, String]]
)

sealed trait UIFormElement

final case class UIFormSection(
    id: UIFormId,
    level: Int,
    messageKey: UIMessageKey,
    children: Seq[UIFormElement],
    decorations: List[UIFieldDecoration],
    repeatIndex: Option[Int]
) extends UIFormElement

final case class UILabeledField(
    id: UIFormId,
    messageKey: UIMessageKey,
    field: UIField,
    decorations: List[UIFieldDecoration]
) extends UIFormElement

final case class UIHiddenField(
    id: UIFormId,
    fieldName: UIFieldName,
    value: Option[String]
) extends UIFormElement

sealed trait UIField:
    def id: UIFormId
    def fieldName: UIFieldName
end UIField

final case class UITextField(
    id: UIFormId,
    fieldName: UIFieldName,
    fieldType: UIFieldType,
    rawValue: Option[String],
    decorations: List[UIFieldDecoration]
) extends UIField

final case class UIFileField(
    id: UIFormId,
    fieldName: UIFieldName,
    fileList: Option[List[UIFile]],
    multiple: Boolean,
    decorations: List[UIFieldDecoration]
) extends UIField

final case class UIChoiceField(
    id: UIFormId,
    fieldName: UIFieldName,
    rawValue: Option[String],
    values: List[UIChoiceOption],
    decorations: List[UIFieldDecoration]
) extends UIField

final case class UIChoiceOption(
    id: UIFormId,
    value: String,
    messageKey: UIMessageKey
)

final case class UIFlexRow(children: Seq[UIFormElement]) extends UIFormElement

final case class UIGrid(children: Seq[Seq[UIGridCell]]) extends UIFormElement

final case class UIGridCell(size: Int, children: Seq[UIFormElement])

final case class UIButton(
    id: UIFormId,
    name: UIFieldName,
    buttonType: String,
    messageKey: UIMessageKey,
    decorations: List[UIFieldDecoration]
) extends UIFormElement

final case class UIBlock(id: UIFormId, messageKey: UIMessageKey) extends UIFormElement
