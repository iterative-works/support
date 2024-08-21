package portaly.forms

import com.raquo.laminar.api.L.*
import works.iterative.ui.model.forms.*
import works.iterative.ui.laminar.*

class UIFormRenderer(cs: Components):
    def render(form: UIForm): HtmlElement =
        inMessageContext(form.messageKey)(
            cs.form(form.id, renderMessage("title"), None, form.children.map(renderSegment))
        )

    private def renderMessage(key: UIMessageKey, suffixes: String*): Node =
        (key.append(suffixes*)).node

    private def renderSegment(segment: UIFormElement): HtmlElement =
        segment match
            case e @ UIFormSection(id, level, messageKey, children, decorations, _) =>
                inMessageContext(messageKey)(
                    cs.section(
                        id,
                        level,
                        Some(renderMessage("section")),
                        Some(renderMessage("section.subtitle")),
                        Val(Nil),
                        children.map(renderSegment)
                    )
                )
            case UILabeledField(id, messageKey, field, decorations) =>
                cs.labeledField(
                    id,
                    renderMessage(messageKey, "label"),
                    None,
                    Val(decorations.contains(UIFieldDecoration.Required)),
                    Val(Nil),
                    inMessageContext(messageKey)(
                        renderField(field)
                    )
                )
            case UIGrid(elems) =>
                cs.grid:
                    elems.flatMap: segments =>
                        segments.map: gridCell =>
                            cs.gridCell(gridCell.size, gridCell.children.map(renderSegment))
            case UIFlexRow(children) =>
                cs.flexRow(children.map(renderSegment))
            case _ => div()

    private def renderField(field: UIField): Node =
        field match
            case UITextField(id, fieldName, fieldType, rawValue, decorations) =>
                cs.inputField(
                    id,
                    fieldName,
                    Val(decorations.contains(UIFieldDecoration.Required))
                )
            case UIFileField(id, fieldName, fileList, multiple, decorations) =>
                div()
            case UIChoiceField(id, fieldName, rawValue, values, decorations) =>
                div()
end UIFormRenderer
